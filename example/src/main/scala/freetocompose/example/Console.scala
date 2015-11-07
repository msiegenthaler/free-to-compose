package freetocompose.example

import freetocompose.FreeToCompose

object ConsoleOps {
  sealed trait ConsoleOp[+A]
  case class Println(text: String) extends ConsoleOp[Unit]
  case class Readln() extends ConsoleOp[String]
}

object Console {
  val functions = FreeToCompose.liftFunctions[ConsoleOps.ConsoleOp]('Console)
}