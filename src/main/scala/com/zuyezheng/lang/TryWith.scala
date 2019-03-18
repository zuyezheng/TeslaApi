package com.zuyezheng.lang

import scala.collection.mutable.ArrayBuffer
import scala.util.control.NonFatal

case class TryWith() {

    val resources: ArrayBuffer[AutoCloseable] = ArrayBuffer()

    def and[A <: AutoCloseable](resource: A): A = {
        this.resources += resource
        resource
    }

    def execute[A](f: TryWith => A): A = {
        var throwable: Option[Throwable] = None
        try {
            f(this)
        } catch {
            case NonFatal(t) =>
                throwable = Some(t)
                throw t
        } finally {
            this.close(throwable)
        }
    }

    def close(optT: Option[Throwable]): Unit = {
        this.resources.foreach(r => {
            try {
                r.close()
            } catch {
                case NonFatal(s) => optT.foreach(t => t.addSuppressed(s))
            }
        })
    }

}