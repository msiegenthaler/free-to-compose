package freetocompose

import scala.language.higherKinds

/** This is a 'specialised' coproduct for operations, because the standard shapeless Coproduct cannot
  * support higher kinded types as needed here. */
case class Combined[F[_], G[_], A](run: Either[F[A], G[A]])

/** This is basically Inject. We use another name to avoid confusion with the general ones provided by
  * shapeless. */
sealed trait Combine[F[_], G[_]] {
  /** Lifts the value into one inside the coproduct. */
  def apply[A](sub: F[A]) = inj(sub)
  def inj[A](sub: F[A]): G[A]
  def prj[A](sup: G[A]): Option[F[A]]
}
object Combine {
  implicit def injRefl[F[_]] = new Combine[F, F] {
    def inj[A](sub: F[A]) = sub
    def prj[A](sup: F[A]) = Some(sup)
  }

  implicit def injLeft[F[_], G[_]] = new Combine[F, Combined[F, G, ?]] {
    def inj[A](sub: F[A]) = Combined(Left(sub))
    def prj[A](sup: Combined[F, G, A]) = sup.run match {
      case Left(fa) => Some(fa)
      case Right(_) => None
    }
  }

  implicit def injRight[F[_], G[_], H[_]](implicit I: Combine[F, G]) = new Combine[F, Combined[H, G, ?]] {
    def inj[A](sub: F[A]) = Combined(Right(I.inj(sub)))
    def prj[A](sup: Combined[H, G, A]) = sup.run match {
      case Left(_) => None
      case Right(x) => I.prj(x)
    }
  }
}
