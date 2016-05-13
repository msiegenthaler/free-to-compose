package freetocompose

import cats.free.{Free, Inject}
import scala.language.higherKinds

/** Support for coproduct based free monads that allow for mixing of multiple 'languages'. */
object Compose {
  /** Lifts an Op into a combined Op-type (F[_]). */
  def lift[Op[_], F[_], A](op: Op[A])(implicit inject: Inject[Op, F]) =
    Free.liftF(inject.inj(op))
}
