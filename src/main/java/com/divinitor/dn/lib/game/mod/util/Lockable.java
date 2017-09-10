package com.divinitor.dn.lib.game.mod.util;

import java.util.concurrent.locks.Lock;

public interface Lockable extends AutoCloseable  {

    static Lockable lock(Lock lock) {
        lock.lock();
        return lock::unlock;
    }

    void close();
}
