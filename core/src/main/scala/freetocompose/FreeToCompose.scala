package freetocompose

import scala.annotation.{StaticAnnotation, compileTimeOnly}
import scala.language.experimental.macros
import scala.language.higherKinds
import scala.reflect.macros.whitebox


/**
  * Usage:
  * <pre>
  * sealed trait Op[+A]
  * case class MyOp(a: String) extends Op[Unit]
  * &commat;AddLiftingFunctions[Op]('Mon) object monadic
  * import monadic._
  * val a: Mon[Unit] = myOp("hello")
  * </pre>
  * @tparam Op sealed trait of the Operations
  */
@compileTimeOnly("enable macro paradise to expand macro annotations")
class AddLiftingFunctions[Op[_]](typeName: Symbol) extends StaticAnnotation {
  def macroTransform(annottees: Any*): Any = macro FreeToCompose.addLiftFunctionsAnnotation_impl
}

/**
  * Usage:
  * <pre>
  *  sealed trait Op[+A]
  * case class MyOp(a: String) extends Op[Unit]
  * val monadic = FreeMacro.liftFunctions[Op]('Mon)
  * import monadic._
  * val a: Mon[Unit] = myOp("hello")
  * </pre>
  */
object FreeToCompose {
  def liftFunctions[F[_]](typeName: Symbol): Any = macro FreeToCompose.liftFunctions_impl[F]
  def liftFunctionsVampire[F[_]](typeName: Symbol): Any = macro FreeToCompose.liftFunctionsVampire_impl[F]
}


//Private stuff below

//Vampire-body, see http://meta.plasm.us/posts/2013/07/12/vampire-methods-for-structural-types/
class vampire(tree: Any) extends StaticAnnotation

class FreeToCompose(val c: whitebox.Context) {
  import c.universe._

  def liftFunctions_impl[F[_]](typeName: Expr[Any])(implicit t: c.WeakTypeTag[F[_]]) =
    generateAnonClass[F](typeName, false)
  def liftFunctionsVampire_impl[F[_]](typeName: Expr[Any])(implicit t: c.WeakTypeTag[F[_]]) =
    generateAnonClass[F](typeName, true)

  private def generateAnonClass[F[_]](typeNameExpr: Expr[Any], vampire: Boolean)(implicit t: c.WeakTypeTag[F[_]]) = {
    val Apply(_, Literal(Constant(typeName: String)) :: Nil) = typeNameExpr.tree
    val mod = generate(TermName(typeName), t.tpe.typeSymbol, false)
    c.Expr(q"new { ..$mod }")
  }

  def addLiftFunctionsAnnotation_impl(annottees: Expr[Any]*): Expr[Any] = {
    val q"new $_[$opIdent](${typeNameTree: Tree}).macroTransform(..$_)" = c.macroApplication
    val opBase = c.typecheck(q"???.asInstanceOf[$opIdent[Unit]]").tpe.typeSymbol
    val Apply(_, Literal(Constant(typeNameString: String)) :: Nil) = typeNameTree
    val typeName = TermName(typeNameString)

    val mod = annottees.map(_.tree).toList match {
      case ClassDef(mods, name, tparams, Template(parents, self, body)) :: rest ⇒ //class/trait
        val (initBody, restBody) = body.splitAt(1)
        val t2 = Template(parents, self, initBody ++ generate(typeName, opBase) ++ restBody)
        ClassDef(mods, name, tparams, t2) :: rest
      case ModuleDef(mods, name, Template(parents, self, body)) :: rest ⇒ // object
        val t2 = Template(parents, self, generate(typeName, opBase) ++ body)
        ModuleDef(mods, name, t2) :: rest
      case a :: rest ⇒
        c.abort(c.enclosingPosition, "AddLiftingFunctions annotation only supported on classes and objects")
    }
    c.Expr(q"..$mod")
  }

  private def generate(name: Name, opBase: Symbol, useVampire: Boolean = false): List[Tree] = {
    val freeTypeName = name.toTypeName
    val freeTypeTree =
      q"""type $freeTypeName[A] =
            _root_.cats.free.Free[${opBase.asType}, A]"""

    val monadDeclTree =
      q"""implicit val monad =
            _root_.cats.free.Free.freeMonad[${opBase.asType}]"""

    val opClass = opBase.asClass
    if (!opClass.isSealed)
      c.abort(c.enclosingPosition, s"The base class ${opBase.name} of the free monad is not sealed")
    if (opClass.knownDirectSubclasses.isEmpty)
      c.abort(c.enclosingPosition, s"The base class ${opBase.name} of the free monad has no subclasses. " +
        s"If you're sure you have subclasses ans use @AddLiftingFunctions then this is a compilation order problem. " +
        s"In that case please use FreeMonad.liftFunctions.")

    val functions = opClass.knownDirectSubclasses.toList.map {
      case s: ClassSymbol ⇒
        forImplementation(opBase.asType.toType, freeTypeName, useVampire)(s)
    }

    freeTypeTree :: monadDeclTree :: functions
  }

  /** Creates "myOp(text: String): FT[Unit]" from "case class MyOp(text: String)" */
  private def forImplementation(base: Type, freeType: TypeName, useVampire: Boolean)(opImpl: ClassSymbol): Tree = {
    // inspired by https://gist.github.com/travisbrown/43c9dc072bfb2bba2611
    val name = TermName(classNameFunctionName(opImpl.name.toString))
    //TODO handle MyOperation[A] extends Op[A]

    val A = opImpl.typeSignature.baseType(base.typeSymbol).typeArgs.head
    val companion = opImpl.companion
    val params = caseClassFields(opImpl.typeSignature)

    if (useVampire) {
      val paramDefs = params.zipWithIndex.map {
        case ((_, tpe), index) ⇒
          val name = TermName("in" + (index + 1))
          q"""$name: $tpe"""
      }
      if (paramDefs.size > 5) c.abort(c.enclosingPosition, s"More parameters in ${companion.name} than supported " +
        "by the FreeMacro. Please tell the maintainer to extend it.")
      val vampire = TermName(s"vampire${paramDefs.size}_impl")
      q"""@_root_.free.vampire($companion)
          def $name(..$paramDefs): $freeType[$A] = macro _root_.freetocompose.FreeToCompose.$vampire"""
    } else {
      val paramNames = params.map(_._1)
      val paramDefs = params.map { p ⇒ q"""${p._1}: ${p._2}""" }
      q"""def $name(..$paramDefs): $freeType[$A] = _root_.cats.free.Free.liftF($companion(..$paramNames))"""
    }
  }


  //Vampire Methods to avoid structural type warning
  def vampire0_impl() =
    q"_root_.cats.free.Free.liftF($companionFromVampire())"
  def vampire1_impl(in1: Expr[Any]) =
    q"_root_.cats.free.Free.liftF($companionFromVampire($in1))"
  def vampire2_impl(in1: Expr[Any], in2: Expr[Any]) =
    q"_root_.cats.free.Free.liftF($companionFromVampire($in1, $in2))"
  def vampire3_impl(in1: Expr[Any], in2: Expr[Any], in3: Expr[Any]) =
    q"_root_.cats.free.Free.liftF($companionFromVampire($in1, $in2, $in3))"
  def vampire4_impl(in1: Expr[Any], in2: Expr[Any], in3: Expr[Any], in4: Expr[Any]) =
    q"_root_.cats.free.Free.liftF($companionFromVampire($in1, $in2, $in3, $in4))"
  def vampire5_impl(in1: Expr[Any], in2: Expr[Any], in3: Expr[Any], in4: Expr[Any], in5: Expr[Any]) =
    q"_root_.cats.free.Free.liftF($companionFromVampire($in1, $in2, $in3, $in4, $in5))"
  private def companionFromVampire = macroAnnotation[vampire].tree.children.tail.head

  /** Current macro Annotation. */
  private def macroAnnotation[T](implicit t: WeakTypeTag[T]): Annotation = {
    c.macroApplication.symbol.annotations.filter(
      _.tree.tpe <:< t.tpe
    ).headOption.getOrElse(c.abort(c.enclosingPosition, s"Annotation ${t.tpe.typeSymbol.name} not found."))
  }

  /** Converts MyOperation to myOperation */
  private def classNameFunctionName(className: String): String = className.head.toLower + className.tail

  /** Extracts [(text, String), (number, Int) from "case class MyClass(text: String, number: Int)" */
  private def caseClassFields(tpe: Type): Iterable[(TermName, Type)] = {
    tpe.decls.collect {
      case accessor: MethodSymbol if accessor.isCaseAccessor ⇒
        accessor.typeSignature match {
          case NullaryMethodType(returnType) ⇒ (accessor.name, returnType)
        }
    }
  }
}