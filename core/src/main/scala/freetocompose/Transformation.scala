package freetocompose

import scala.language.higherKinds
import cats._

class CombinedTransformation[F[_], G[_], H[_]](f: F ~> H, g: G ~> H) extends (Combined[F, G, ?] ~> H) {
  type From[A] = Combined[F, G, A]
  def apply[A](fa: From[A]) = fa.run match {
    case Left(fa) ⇒ f(fa)
    case Right(fa) ⇒ g(fa)
  }
  def ||[I[_]](i: I ~> H) = new CombinedTransformation[From, I, H](this, i)
}