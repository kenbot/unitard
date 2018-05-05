package unitard

import org.scalacheck.util.Pretty
import org.scalacheck.{Shrink, Gen}

import org.scalacheck.Gen._
import org.scalacheck.Arbitrary._
import JavaInterop._

import scala.collection.JavaConverters._

object Generators {

  def genPathKey: Gen[AnyRef] =
    oneOf(Gen.alphaStr, Gen.choose(0, 10), const(null))

  def genKleisliFn: Gen[JFunc[String, Hopefully[String]]] =
    genHopefully.map(h => JFunc(s => h.map(JFunc(_ + s))))

  def genFn: Gen[JFunc[String, String]] =
      arbitrary[String].map(a => JFunc(s => s + a + "!!!"))

  def genPath: Gen[Path] =
    Gen.listOf(genPathKey).map(lst => Path.of(lst: _*))

  def genJBool: Gen[JBool] =
    arbitrary[Boolean].map(b => new JBool(b))

  def genJInt: Gen[JInt] =
    arbitrary[Int].map(i => new JInt(i))

  def genJDouble: Gen[JDouble] =
    arbitrary[Double].map(d => new JDouble(d))

  def genJavaPrimitive: Gen[AnyRef] =
    oneOf(arbitrary[String], genJInt, genJBool, genJDouble, const(null))

  def genStuffValue: Gen[Object] = {
    def genStuffValue1: Gen[Object] = {
      def genJavaList2: Gen[JList[Object]] =
        listOf(genJavaPrimitive).map(_.asJava)

      def genJavaMap2: Gen[JMap[Object, Object]] = mapOf(for {
        key <- genJavaPrimitive
        value <- genJavaPrimitive
      } yield (key, value)).map(_.asJava)

      frequency(
        10 -> genJavaPrimitive,
        1 -> genJavaList2,
        1 -> genJavaMap2)
    }

    def genJavaList1: Gen[JList[Object]] =
      listOf(genStuffValue1).map(_.asJava)

    def genJavaMap1: Gen[JMap[Object, Object]] = mapOf(for {
      key <- genJavaPrimitive
      value <- genStuffValue1
    } yield (key, value)).map(_.asJava)

    frequency(
      10 -> genJavaPrimitive,
      1 -> genJavaList1,
      1 -> genJavaMap1)
  }

  def genJavaList: Gen[JList[Object]] =
    listOf(genStuffValue).map(_.asJava)

  def genJavaMap: Gen[JMap[Object, Object]] = mapOf(for {
    key <- genJavaPrimitive
    value <- genStuffValue
  } yield (key, value)).map(_.asJava)

  def genListStuff: Gen[Stuff] =
    genJavaList.map(Stuff.fromList)

  def genMapStuff: Gen[Stuff] =
    genJavaMap.map(Stuff.fromMap)

  def genStuff: Gen[Stuff] = oneOf(genListStuff, genMapStuff)

  implicit def shrinkStuff: Shrink[Stuff] =
    Shrink(stuff =>
      stuff.getKeys.asScala.toStream.map(k => stuff.remove(k)))

  def genHopefully: Gen[Hopefully[String]] = {
    val a: Gen[Hopefully[String]] = for {
      p <- genPath
      a <- arbitrary[String]
    } yield Hopefully.notNull(a) withPath p

    val b: Gen[Hopefully[String]] =
      genPath.map(p => Hopefully.notNull[String](null) withPath p)

    val c: Gen[Hopefully[String]] =
      genPath.map(p => Hopefully.missing() withPath p)

    case object Banana
    val d: Gen[Hopefully[String]] = const(Hopefully.notNull(Banana)).map(_.as(classOf[String]))

    oneOf(a, b, c, d)
  }


  implicit def prettyObject(o: Object): Pretty = Pretty(String.valueOf)
}
