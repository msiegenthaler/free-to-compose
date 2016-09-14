package freetocompose.example

import scala.language.higherKinds
import cats.Monad
import cats.instances.list._
import cats.syntax.foldable._

object Utils {
  def repeat[M[_]: Monad](times: Int)(fa: M[_]) = List.fill(times)(fa).sequence_
  def iterateUntil[M[_]: Monad, A](pred: A ⇒ Boolean)(fa: M[A]): M[A] =
    Monad[M].flatMap(fa)(y ⇒ if (pred(y)) Monad[M].pure(y) else iterateUntil(pred)(fa))
}
