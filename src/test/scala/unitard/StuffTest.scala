package unitard

import java.util.function.Consumer

import org.scalacheck.Prop
import org.specs2.ScalaCheck
import org.specs2.mutable.Specification
import Generators._
import org.scalacheck.Arbitrary.arbitrary
import JavaInterop._


class StuffTest extends Specification with ScalaCheck {
  "List stuff" should {
    "be empty when empty" in {
      Stuff.EMPTY_LIST.isEmpty should beTrue
    }
  }

  "Map stuff" should {
    "be empty when empty" in {
      Stuff.EMPTY_MAP.isEmpty should beTrue
    }

    "return an element that has been set (PUT-GET)" in {
      Prop.forAll(genMapStuff, genJavaPrimitive, genJavaPrimitive) {
        (stuff, k, v) => stuff
          .put(k, v)
          .get(k) === Hopefully.notNull(v).withPath(Path.of(k))
      }
    }

    "return the most recent element that has been set (PUT-PUT)" in {
      Prop.forAll(genMapStuff, genJavaPrimitive, genJavaPrimitive, genJavaPrimitive) {
        (stuff, k, v1, v2) => stuff
          .put(k, v1)
          .put(k, v2)
          .get(k) === Hopefully.notNull(v2).withPath(Path.of(k))
      }
    }

    "Be unchanged if the same thing gets set again (GET-PUT)" in {
      Prop.forAll(genMapStuff, genJavaPrimitive, genJavaPrimitive) {
        (stuff, k, v) =>
          val stuff2 = stuff.put(k, v)
          val stuff3 = stuff2.put(k, v)

          stuff2 === stuff3
      }
    }

    "Be unchanged if you rebuild from the iterator" in {
      Prop.forAll(genMapStuff) {
        stuff =>
          val jmap: JMap[Object,Object] = JMap()

          stuff.iterator().forEachRemaining(new Consumer[Entry] {
            def accept(e: Entry): Unit =
              jmap.put(e.getKey, e.getValue)
          })

          Stuff.fromMap(jmap) === stuff
      }
    }
  }

  "Something placed at a path" should {
    "be retrievable" in {
      Prop.forAll(arbitrary[String], arbitrary[String]) { (a, b) =>
        val child = Stuff.mapOf(b, "banana")
        val root = Stuff.mapOf(a, child)

        root.get(a, b).unsafeGet() === "banana"
      }
    }
  }

  "Putting the same thing a second time" should {
    "not change the size" in {
      Prop.forAll(genStuff, genJavaPrimitive, genJavaPrimitive) {
        (stuff, key, value) =>
          val stuff2 = stuff.put(key, value)
          val size = stuff2.size
          stuff2.put(key, value).size === size
      }
    }
  }

  "return the most recently set element" in {
    Prop.forAll(genMapStuff, genJavaPrimitive, genJavaPrimitive, genJavaPrimitive) {
      (stuff, k, v1, v2) => stuff
        .put(k, v1)
        .put(k, v2)
        .get(k) === Hopefully.notNull(v2).withPath(Path.of(k))
    }

  }
}
