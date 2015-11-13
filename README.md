Free-to-Compose
===============

Library that eases the usage of Free Monads based on cats and allows to compose multiple free monads into one.

It provides macros to automatically generate the lifting functions and composable lifting functions. The library
can be used in two ways: simple lifting functions and a composable variant based on Rúnar's talk
"reasonably priced monads".


Simple Usage
------------
Declare your Ops (the F[_]):

    object ConsoleOps {
      sealed trait ConsoleOp[+A]
      case class Println(text: String) extends ConsoleOp[Unit]
      case class Readln() extends ConsoleOp[String]
    }
    
then have the macro generate the lifting functions:

    import freetocompose.FreeToCompose
    object Console {
      @addLiftingFunctions[ConsoleOps.ConsoleOp]('Console) object functions
    }


Console.functions will contain the following definitions:
- type Console[+A] = Free[ConsoleOp, A]  //Console is the name you specified as the parameter
- def println(text: String): Console[Unit]
- def readln(): Console[String]

Use the lifted functions like usual:

    import Console.functions._
    val program: Console[String] = {
      for {
        _ <- println("Please tell me your name (empty to exit):")
        greeting = "Hello"
        name <- readln
        _ <- println(s"$greeting $name")
      } yield name
    }

to run it use:

    val pgm = program.foldMap(ConsoleCompile.toTrampoline)
    pgm.run


for the complete example see the code under [Console.scala](example/src/main/scala/freetocompose/example/Console.scala) and
[ConsoleExample.scala](example/src/main/scala/freetocompose/example/ConsoleExample.scala)



Composable Usage
----------------
The example shown above works great in situation where you have a single resource or IO-type. But let's say we want
to build a simple hotel reservation system that needs to communicate with the customer and keep track of which
rooms are already occupied inside a persistent database.

We keep the ConsoleOps described above and add a second free monad called store:

    object StoreOps {
      sealed trait StoreOp[+A]
      case class Put(key: String, value: String) extends StoreOp[Unit]
      case class Get(key: String) extends StoreOp[Option[String]]
    }

we again use a macro to generate the lifting functions but this time we'll use composing lifting functions:

    object Console {
      @addComposingFunctions[ConsoleOps.ConsoleOp]('Console) object composing
    }
    object Store {
      @addComposingFunctions[StoreOps.StoreOp]('Store) object composing
    }


The definitions generated inside Console (Store is equivalent) are:
- type Console[F[_]] = Combine[ConsoleOp, F]
- def println[F[_] : Console](text: String): Free[F, Unit]
- def readln[F[_] : Console](): Free[F, String]


When using we may now combine the two different kinds of operations into one monad:

    def hotelCheckin[F[_] : Console : Store] = for {                    //need to declare all op-types used as context bounds
      hotel <- get("hotel")                                             //Store Op
      _ <- println(s"Welcome to the $hotel, please enter your name:")   //Console Op
      name <- readln()                                                  //Console Op
      _ <- put("customer", name)                                        //Store Op
    } yield name


of course you then also need to combine the Compilers (= Transforms  = cats.~>):

     val compiler = ConsoleCompile.toTrampoline || StoreCompile.toTrampoline()
     val program = assignRoom[compiler.From].foldMap(compiler)
     program.run

for the complete example see the code under [Console.scala](example/src/main/scala/freetocompose/example/Console.scala),
[Store.scala](example/src/main/scala/freetocompose/example/Store.scala) and
[Example.scala](example/src/main/scala/freetocompose/example/Example.scala)



Implementation
--------------
The implementation is actually rather simple (well, macros are always a bit involved, but the generated code is).
- Combined is just a simplified scalaz-Coproduct and Combine a scalaz-Inject.
- the Ops get lifted into a coproduct of Ops (if it where shapeless we'd write Console :+: Store :+: CNil)
- the macro is just there to reduce the boilerplate with the type and lifting function definitions.

So, no black magic involved.




Credits and further readings
----------------------------
- Idea for the macro by Travis Brown: https://gist.github.com/travisbrown/43c9dc072bfb2bba2611
- Idea for composition via Coproducts Rúnar Bjarnason (@runarorama): http://functionaltalks.org/2014/11/23/runar-oli-bjarnason-free-monad/
- How to fix the knownDirectSubclasses problem by Lloyd: https://github.com/lloydmeta/enumeratum
- The great cats library: https://github.com/non/cats
