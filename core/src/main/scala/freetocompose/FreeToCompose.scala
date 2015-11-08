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

@compileTimeOnly("enable macro paradise to expand macro annotations")
class AddComposingFunctions[Op[_]](typeName: Symbol) extends StaticAnnotation {
  def macroTransform(annottees: Any*): Any = macro FreeToCompose.addComposingFunctionsAnnotation_impl
}

/**
  * Usage:
  * <pre>
  * sealed trait Op[+A]
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

class FreeToCompose(val c: whitebox.Context) {
  import c.universe._
  import Describe._

  def liftFunctions_impl[F[_]](typeName: Expr[Any])(implicit t: c.WeakTypeTag[F[_]]) = {
    val desc = Describe(t.tpe.typeSymbol)
    val alias = macroParameter(typeName.tree)

    anonClass(typeAlias(alias, desc) ::
      monadDefinition(desc) ::
      desc.ops.map(liftedFunction(alias, _)))
  }

  def liftFunctionsVampire_impl[F[_]](typeName: Expr[Any])(implicit t: c.WeakTypeTag[F[_]]) = {
    val desc = Describe(t.tpe.typeSymbol)
    val alias = macroParameter(typeName.tree)

    anonClass(typeAlias(alias, desc) ::
      monadDefinition(desc) ::
      desc.ops.map(liftedFunctionWithVampire(alias, _)))
  }


  def addLiftFunctionsAnnotation_impl(annottees: Expr[Any]*) = {
    val (opBase, alias) = parseAnnotation
    val desc = Describe(opBase)

    modifyObjectOrClass(annottees,
      typeAlias(alias, desc) ::
        monadDefinition(desc) ::
        desc.ops.map(liftedFunction(alias, _)))
  }


  def addComposingFunctionsAnnotation_impl(annottees: Expr[Any]*) = {
    val (opBase, alias) = parseAnnotation
    val desc = Describe(opBase)

    val importHigherKinds =
      q"import scala.language.higherKinds"
    val typeAlias =
      q"type $alias[F[_]] = _root_.freetocompose.Compose.Combine[${desc.opBase.typeSymbol}, F]"

    def function(op: Op) = {
      val paramNames = op.params.map(_.name)
      val paramDefs = op.params.map { p ⇒ q"${p.name}: ${p.tpe}" }
      q"""def ${op.functionName}[F[_] : $alias](..$paramDefs): _root_.cats.free.Free[F, ${op.opA}] =
          _root_.freetocompose.Compose.lift(${op.companion}(..$paramNames))"""
    }

    modifyObjectOrClass(annottees,
      importHigherKinds ::
        typeAlias ::
        desc.ops.map(function))
  }


  private def anonClass(of: List[Tree]): Tree = q"new {..$of}"

  private def modifyObjectOrClass(annottees: Traversable[Expr[Any]], toAdd: List[Tree]): Expr[Any] = {
    val mod = annottees.map(_.tree).toList match {
      case ClassDef(mods, name, tparams, Template(parents, self, body)) :: rest ⇒ //class/trait
        val (initBody, restBody) = body.splitAt(1)
        val t2 = Template(parents, self, initBody ++ toAdd ++ restBody)
        ClassDef(mods, name, tparams, t2) :: rest
      case ModuleDef(mods, name, Template(parents, self, body)) :: rest ⇒ // object
        val t2 = Template(parents, self, toAdd ++ body)
        ModuleDef(mods, name, t2) :: rest
      case a :: rest ⇒
        c.abort(c.enclosingPosition, "Annotation only supported on classes and objects")
    }
    c.Expr(q"..$mod")
  }

  private def parseAnnotation: (Symbol, TypeName) = {
    val q"new $_[$opIdent](${typeName: Tree}).macroTransform(..$_)" = c.macroApplication
    val opBase = c.typecheck(q"???.asInstanceOf[$opIdent[Unit]]").tpe.typeSymbol
    val alias = macroParameter(typeName)
    (opBase, alias)
  }
  private def macroParameter(tree: Tree) = {
    val Apply(_, Literal(Constant(typeName: String)) :: Nil) = tree
    TypeName(typeName)
  }
  private def macroAnnotation[T](implicit t: WeakTypeTag[T]): Annotation = {
    c.macroApplication.symbol.annotations.filter(
      _.tree.tpe <:< t.tpe
    ).headOption.getOrElse(c.abort(c.enclosingPosition, s"Annotation ${t.tpe.typeSymbol.name} not found."))
  }

  private def typeAlias(name: TypeName, desc: Description): Tree = {
    q"type $name[A] = _root_.cats.free.Free[${desc.opBase.typeSymbol}, A]"
  }

  private def monadDefinition(desc: Description): Tree = {
    q"""implicit val monad = _root_.cats.free.Free.freeMonad[${desc.opBase.typeSymbol}]"""
  }

  private def liftedFunction(typeAlias: TypeName, op: Op): Tree = {
    val paramNames = op.params.map(_.name)
    val paramDefs = op.params.map { p ⇒ q"${p.name}: ${p.tpe}" }
    q"""def ${op.functionName}(..$paramDefs): $typeAlias[${op.opA}] =
          _root_.cats.free.Free.liftF(${op.companion}(..$paramNames))"""
  }

  private def liftedFunctionWithVampire(typeAlias: TypeName, op: Op): Tree = {
    val paramDefs = op.params.zipWithIndex.map {
      case (Field(_, tpe), index) ⇒
        val name = TermName("in" + (index + 1))
        q"""$name: $tpe"""
    }
    if (paramDefs.size > 5) {
      c.abort(c.enclosingPosition, s"More parameters in ${op.name} than supported " +
        "by the FreeMacro. Please tell the maintainer to extend it.")
    }
    val vampire = TermName(s"vampire${paramDefs.size}_impl")
    q"""@_root_.freetocompose.vampire(${op.companion})
        def ${op.functionName}(..$paramDefs): $typeAlias[${op.opA}] =
          macro _root_.freetocompose.FreeToCompose.$vampire"""
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


  /** Describes an operation hierarchy. */
  private object Describe {
    case class Description(opBase: Type, ops: List[Op])
    case class Op(name: TypeName, companion: Symbol, opA: Type, params: List[Field]) {
      /** myOperation for MyOperation */
      def functionName = {
        val className = name.toString
        TermName(className.head.toLower + className.tail)
      }
    }
    case class Field(name: TermName, tpe: Type)

    def apply(opBase: Type): Description = apply(opBase.typeSymbol)

    def apply(opBase: Symbol): Description = {
      val opBaseClass = opBase.asClass

      if (!opBaseClass.isSealed)
        c.abort(c.enclosingPosition, s"The base class ${opBase.name} of the free monad is not sealed")
      if (opBaseClass.knownDirectSubclasses.isEmpty)
        c.abort(c.enclosingPosition, s"The base class ${opBase.name} of the free monad has no subclasses. " +
          s"If you're sure you have subclasses ans use @AddLiftingFunctions then this is a compilation order problem. " +
          s"In that case please use FreeMonad.liftFunctions.")
      val ops = opBaseClass.knownDirectSubclasses.toList.map { case s: ClassSymbol ⇒ describeOp(s, opBaseClass) }

      Description(opBase.asType.toType, ops)
    }

    /** Describes a "case class MyOp(text: String) extends Op[Unit]" */
    private def describeOp(opClass: ClassSymbol, opBase: ClassSymbol): Op = {
      val name = opClass.name
      val companion = opClass.companion
      val a = opClass.typeSignature.baseType(opBase.asType).typeArgs.head
      val params = caseClassFields(opClass.typeSignature)
      Op(name, companion, a, params.toList)
    }

    /** Extracts [Field(text, String), Field(number, Int)] from a "case class MyClass(text: String, number: Int)" */
    private def caseClassFields(tpe: Type): Iterable[Field] = {
      tpe.decls.collect {
        case accessor: MethodSymbol if accessor.isCaseAccessor ⇒
          accessor.typeSignature match {
            case NullaryMethodType(returnType) ⇒ Field(accessor.name, returnType)
          }
      }
    }
  }
}

//Vampire-body, see http://meta.plasm.us/posts/2013/07/12/vampire-methods-for-structural-types/
class vampire(tree: Any) extends StaticAnnotation
