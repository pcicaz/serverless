/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements. See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.bluenimble.platform.pooling;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;

public class ObjectPool<T> {

    protected final PoolConfig config;
    protected final ObjectFactory<T> factory;
    protected final ObjectPoolPartition<T>[] partitions;
    private Scavenger scavenger;
    private volatile boolean shuttingDown;

    @SuppressWarnings("unchecked")
	public ObjectPool(PoolConfig poolConfig, ObjectFactory<T> objectFactory) {
        this.config = poolConfig;
        this.factory = objectFactory;
        this.partitions = new ObjectPoolPartition[config.getPartitionSize()];
        try {
            for (int i = 0; i < config.getPartitionSize(); i++) {
                partitions[i] = new ObjectPoolPartition<>(this, i, config, objectFactory, createBlockingQueue(poolConfig));
            }
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
        if (config.getScavengeIntervalMilliseconds() > 0) {
            this.scavenger = new Scavenger();
            this.scavenger.start();
        }
    }

    protected BlockingQueue<Poolable<T>> createBlockingQueue(PoolConfig poolConfig) {
        return new ArrayBlockingQueue<>(poolConfig.getMaxSize());
    }

    public Poolable<T> borrowObject() {
        return borrowObject(true);
    }

    public Poolable<T> borrowObject(boolean blocking) {
        for (int i = 0; i < 3; i++) { // try at most three times
            Poolable<T> result = getObject(blocking);
            if (factory.validate(result.getObject())) {
                return result;
            } else {
                this.partitions[result.getPartition()].decreaseObject(result);
            }
        }
        throw new RuntimeException("Cannot find a valid object");
    }

    private Poolable<T> getObject(boolean blocking) {
        if (shuttingDown) {
            throw new IllegalStateException("Your pool is shutting down");
        }
        int partition = (int) (Thread.currentThread().getId() % this.config.getPartitionSize());
        ObjectPoolPartition<T> subPool = this.partitions[partition];
        Poolable<T> freeObject = subPool.getObjectQueue().poll();
        if (freeObject == null) {
            // increase objects and return one, it will return null if reach max size
            subPool.increaseObjects(1);
            try {
                if (blocking) {
                    freeObject = subPool.getObjectQueue().take();
                } else {
                    freeObject = subPool.getObjectQueue().poll(config.getMaxWaitMilliseconds(), TimeUnit.MILLISECONDS);
                    if (freeObject == null) {
                        throw new RuntimeException("Cannot get a free object from the pool");
                    }
                }
            } catch (InterruptedException e) {
                throw new RuntimeException(e); // will never happen
            }
        }
        freeObject.setLastAccessTs(System.currentTimeMillis());
        return freeObject;
    }

    public void returnObject(Poolable<T> obj) {
        ObjectPoolPartition<T> subPool = this.partitions[obj.getPartition()];
        try {
            subPool.getObjectQueue().put(obj);
        } catch (InterruptedException e) {
            throw new RuntimeException(e); // impossible for now, unless there is a bug, e,g. borrow once but return twice.
        }
    }

    public int getSize() {
        int size = 0;
        for (ObjectPoolPartition<T> subPool : partitions) {
            size += subPool.getTotalCount();
        }
        return size;
    }

    public synchronized int shutdown() throws InterruptedException {
        shuttingDown = true;
        int removed = 0;
        if (scavenger != null) {
            scavenger.interrupt();
            scavenger.join();
        }
        for (ObjectPoolPartition<T> partition : partitions) {
            removed += partition.shutdown();
        }
        return removed;
    }

    private class Scavenger extends Thread {

        @Override
        public void run() {
            int partition = 0;
            while (!ObjectPool.this.shuttingDown) {
                try {
                    Thread.sleep(config.getScavengeIntervalMilliseconds());
                    partition = ++partition % config.getPartitionSize();
                    partitions[partition].scavenge();
                } catch (InterruptedException ignored) {
                }
            }
        }

    }
}
