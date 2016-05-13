package freetocompose

import scala.language.higherKinds
import cats._
import cats.data.{ Coproduct, Xor }

class CombinedTransformation[F[_], G[_], H[_]](f: F ~> H, g: G ~> H) extends (Coproduct[F, G, ?] ~> H) {
  type From[A] = Coproduct[F, G, A]
  def apply[A](fa: From[A]) = fa.run match {
    case Xor.Left(fa) ⇒ f(fa)
    case Xor.Right(fa) ⇒ g(fa)
  }
  def ||[I[_]](i: I ~> H) = new CombinedTransformation[From, I, H](this, i)
}