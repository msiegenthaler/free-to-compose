package freetocompose.example

import scala.language.higherKinds
import freetocompose.Compose.Combined
import Console.composing._, ConsoleOps.ConsoleOp
import Store.composing._, StoreOps.StoreOp

object Example {
  def ask[F[_] : Console](prompt: String) = for {
    _ <- println(prompt)
    in <- readln()
  } yield in

  def askForName[F[_] : Console : Store] = for {
    name <- ask("Welcome, please enter your name:")
    _ <- put("name", name)
  } yield name

  def assignRoom[F[_] : Console : Store] = for {
    name <- askForName
    room <- get("nextRoom")
    _ <- {
      if (room.isDefined) println(s"Hi $name, you have been assigned room $room")
      else println("Sorry, we have no rooms")
    }
  } yield ()


  def main(args: Array[String]): Unit = {
    type App[A] = Combined[ConsoleOp, StoreOp, A]
    SideEffectStore.store.put("nextRoom", "Berlin")
    val compiler = ConsoleInterpreterSysout.Compiler || SideEffectStore.Compiler
    val program = assignRoom[compiler.From].foldMap(compiler)
    program
  }
}