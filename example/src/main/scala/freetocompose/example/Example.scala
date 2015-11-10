package freetocompose.example

import scala.language.higherKinds
import Console.composing._
import Store.composing._

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
}