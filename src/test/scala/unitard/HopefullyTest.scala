package unitard


import org.specs2.ScalaCheck
import org.specs2.mutable.Specification

import org.scalacheck.Prop
import JavaInterop._


class HopefullyTest extends Specification with ScalaCheck {
  "Hopefully null value" should {
    "be null" in {
      Hopefully.notNull(null).isNull should beTrue
    }

    "unsafely resolve to null" in {
      Hopefully.notNull[Any](null).unsafeGet === null
    }
  }

  "Hopefully not-null value" should {
    "be an actual value" in {
      Hopefully.notNull("hi").isActualValue should beTrue
    }

    "unsafely resolve to the same value" in {
      Hopefully.notNull("hi").unsafeGet === "hi"
    }
  }

  import Generators._

  "flatMap" should {
    "be a no-op with respect to notNull()" in {
      Prop.forAll(genHopefully) {
        h => h.flatMap[String](JFunc(s => Hopefully.notNull(s))) should beEqualTo(h)
      }
    }

    "be associative" in {
      Prop.forAll(genHopefully, genKleisliFn, genKleisliFn) {
        (h, f, g) =>
          h.flatMap(f).flatMap(g) === h.flatMap(JFunc(a => f(a).flatMap(g)))
      }
    }
  }

  "map" should {
    "be a no-op with respect to the identity function" in {
      Prop.forAll(genHopefully) {
        h => h.map[String](JFunc(identity)) should beEqualTo(h)
      }
    }

    "compose" in {
      Prop.forAll(genHopefully, genFn, genFn) {
        (h, f, g) =>
          h.map(f).map(g) === h.map(JFunc(a => g(f(a))))
      }
    }
  }

}
