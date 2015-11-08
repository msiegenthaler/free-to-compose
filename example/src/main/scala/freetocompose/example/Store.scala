package freetocompose.example

import freetocompose.{addLiftingFunctions, addComposingFunctions}

object StoreOps {
  sealed trait StoreOp[+A]
  case class Put(key: String, value: String) extends StoreOp[Unit]
  case class Get(key: String) extends StoreOp[String]
}

object Store {
  @addLiftingFunctions[StoreOps.StoreOp]('Store) object functions
  @addComposingFunctions[StoreOps.StoreOp]('Store) object composing
}
