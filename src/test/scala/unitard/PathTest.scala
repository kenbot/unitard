package unitard

import org.scalacheck.Prop
import org.specs2.ScalaCheck
import org.specs2.mutable.Specification
import unitard.JavaInterop._
import org.scalacheck.Gen

class PathTest extends Specification with ScalaCheck {
  import Generators._

  "An empty path" should {
    "be empty" in {
      Path.EMPTY.isEmpty should beTrue
    }

    "be neutral when prepended to another path" in {
      Prop.forAll(genPath) {
        p => Path.EMPTY.join(p) === p
      }
    }

    "be neutral when appended to another path" in {
      Prop.forAll(genPath) {
        p => p.join(Path.EMPTY) === p
      }
    }
  }

  "Joining paths" should {
    "be associative" in {
      val gen3Paths = for {
        a <- genPath
        b <- genPath
        c <- genPath
      } yield (a,b,c)
        //Apply[Gen].apply3(genPath, genPath, genPath)((_,_,_))

      Prop.forAll(gen3Paths) {
        case (p1, p2, p3) => p1.join(p2.join(p3)) === p1.join(p2).join(p3)
      }
    }
  }

  "A path containing null keys" should {
    "render to string without explosions" in {
      Path.of(null, null, null).toString
      true
    }
  }


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
