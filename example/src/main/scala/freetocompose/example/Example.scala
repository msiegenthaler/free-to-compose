package freetocompose.example

import scala.language.higherKinds
import freetocompose.Compose._
import freetocompose.example.ConsoleOps._
import freetocompose.example.StoreOps._


object Impls {
  type Console[F[_]] = Combine[ConsoleOp, F]

  def println[F[_] : Console](text: String) = lift(Println(text))
  def readln[F[_] : Console]() = lift(Readln())

  type Store[F[_]] = Combine[StoreOp, F]
  def put[F[_] : Store](key: String, value: String) = lift(Put(key, value))
  def get[F[_] : Store](key: String) = lift(Get(key))
}

object Usage {
  import Impls._

  def askForName[F[_] : Console : Store] = for {
    _ <- println("Welcome, please enter your name:")
    name <- readln()
    _ <- put("name", name)
  } yield name

  def assignRoom[F[_] : Console : Store] = for {
    name <- askForName
    room <- get("nextRoom")
    _ <- println(s"Hi $name, you have been assigned room $room")
  } yield ()
}

object UsageWithMacro {
  import Console.composing._
  import Store.composing._

  def askForName[F[_] : Console : Store] = for {
    _ <- println("Welcome, please enter your name:")
    name <- readln()
    _ <- put("name", name)
  } yield name

  def assignRoom[F[_] : Console : Store] = for {
    name <- askForName
    room <- get("nextRoom")
    _ <- println(s"Hi $name, you have been assigned room $room")
  } yield ()
}