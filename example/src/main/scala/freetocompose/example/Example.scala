package freetocompose.example

import scala.language.higherKinds
import freetocompose.Combined
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

  private val rooms = (100 to 200).map(_.toString)
  def nextRoom[F[_] : Store] = for {
    roomOpt ← get("lastRoom")
    room = roomOpt.fold(rooms.head)((r: String) ⇒ rooms.dropWhile(_ != r).drop(1).head)
    _ ← put("lastRoom", room)
  } yield room

  def assignRoom[F[_] : Console : Store] = for {
    name <- askForName
    room <- nextRoom
    _ <- println(s"Hi $name, you have been assigned room $room")
  } yield ()


  def main(args: Array[String]): Unit = {
    type App[A] = Combined[ConsoleOp, StoreOp, A]
    val compiler = ConsoleCompile.toId || StoreCompile.toId()
    val program = assignRoom[compiler.From].foldMap(compiler)
    program
  }
}