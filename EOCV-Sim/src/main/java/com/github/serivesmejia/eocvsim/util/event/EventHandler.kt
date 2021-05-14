/*
 * Copyright (c) 2021 Sebastian Erives
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 *
 */

package com.github.serivesmejia.eocvsim.util.event

import com.github.serivesmejia.eocvsim.util.Log

class EventHandler(val name: String) : Runnable {

    private val lock = Any()
    private val onceLock = Any()

    val listeners: Array<EventListener>
        get()  {
            synchronized(lock) {
                return internalListeners.toTypedArray()
            }
        }

    val onceListeners: Array<EventListener>
        get() {
            synchronized(onceLock) {
                return internalOnceListeners.toTypedArray()
            }
        }

    private val internalListeners     = ArrayList<EventListener>()
    private val internalOnceListeners = ArrayList<EventListener>()

    override fun run() {
        for(listener in listeners) {
            try {
                listener.run(EventListenerRemover(this, listener))
            } catch (ex: Exception) {
                Log.warn("${name}-EventHandler", "Error while running listener ${listener.javaClass.name}", ex)
            }
        }

        val toRemoveOnceListeners = mutableListOf<EventListener>()

        //executing "doOnce" listeners
        for(listener in onceListeners) {
            try {
                listener.run(EventListenerRemover(this, listener))
            } catch (ex: Exception) {
                Log.warn("${name}-EventHandler", "Error while running \"once\" ${listener.javaClass.name}", ex)
            }

            toRemoveOnceListeners.add(listener)
        }

        synchronized(onceLock) {
            for(listener in toRemoveOnceListeners) {
                internalOnceListeners.remove(listener)
            }
        }
    }

    fun doOnce(listener: EventListener) = synchronized(onceLock) {
        internalOnceListeners.add(listener)
    }

    fun doOnce(runnable: Runnable) = doOnce { runnable.run() }

    fun doPersistent(listener: EventListener) = synchronized(lock) {
        internalListeners.add(listener)
    }

    fun doPersistent(runnable: Runnable) = doPersistent { runnable.run() }

    fun removePersistentListener(listener: EventListener) {
        if(internalListeners.contains(listener)) {
            synchronized(lock) { internalListeners.remove(listener) }
        }
    }

    fun removeOnceListener(listener: EventListener) {
        if(internalOnceListeners.contains(listener)) {
            synchronized(onceLock) { internalOnceListeners.remove(listener) }
        }
    }

    fun removeAllListeners() {
        removeAllPersistentListeners()
        removeAllOnceListeners()
    }

    fun removeAllPersistentListeners() = synchronized(lock) {
        internalListeners.clear()
    }

    fun removeAllOnceListeners() = synchronized(onceLock) {
        internalOnceListeners.clear()
    }

}