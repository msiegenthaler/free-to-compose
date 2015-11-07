package freetocompose.example

import scala.language.higherKinds
import cats.free.Free
import freetocompose.example.ConsoleOps._
import freetocompose.example.StoreOps._


case class Coproduct[F[_], G[_], A](run: Either[F[A], G[A]])

sealed trait Inject[F[_], G[_]] {
  def apply[A](sub: F[A]) = inj(sub)
  def inj[A](sub: F[A]): G[A]
  def prj[A](sup: G[A]): Option[F[A]]
}
object Inject {
  implicit def injRefl[F[_]] = new Inject[F, F] {
    def inj[A](sub: F[A]) = sub
    def prj[A](sup: F[A]) = Some(sup)
  }

  implicit def injLeft[F[_], G[_]] = new Inject[F, ({type λ[α] = Coproduct[F, G, α]})#λ] {
    def inj[A](sub: F[A]) = Coproduct(Left(sub))
    def prj[A](sup: Coproduct[F, G, A]) = sup.run match {
      case Left(fa) => Some(fa)
      case Right(_) => None
    }
  }

  implicit def injRight[F[_], G[_], H[_]](implicit I: Inject[F, G]) =
    new Inject[F, ({type f[x] = Coproduct[H, G, x]})#f] {
      def inj[A](sub: F[A]) = Coproduct(Right(I.inj(sub)))
      def prj[A](sup: Coproduct[H, G, A]) = sup.run match {
        case Left(_) => None
        case Right(x) => I.prj(x)
      }
    }
}


object Compose {
  class Consoles[F[_]](implicit i: Inject[ConsoleOp, F]) {
    def println(text: String) = lift(Println(text))
    def readln() = lift(Readln())
    private def lift[A](op: ConsoleOp[A]): Free[F, A] = Free.liftF(i(op))
  }
  class Stores[F[_]](implicit i: Inject[StoreOp, F]) {
    def put(key: String, value: String) = lift(Put(key, value))
    def get(key: String) = lift(Get(key))
    private def lift[A](op: StoreOp[A]): Free[F, A] = Free.liftF(i(op))
  }
}

object Usage {
  import Compose._

  def program[F[_]](implicit C: Consoles[F], S: Stores[F]): Free[F, Unit] = {
    import C._
    import S._
    for {
      _ <- println("Welcome, please enter your name:")
      name <- readln()
      _ <- put("name", name)
    } yield ()
  }
}