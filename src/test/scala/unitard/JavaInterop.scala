package unitard

object JavaInterop {

  type JFunc[A,B] = java.util.function.Function[A,B]
  type JList[A] = java.util.List[A]
  type JMap[K,V] = java.util.Map[K,V]
  type JInt = java.lang.Integer
  type JBool = java.lang.Boolean
  type JDouble = java.lang.Double

  def JFunc[A,B](f: A => B, desc: String = "fn"): JFunc[A,B] = new JFunc[A,B] {
    def apply(a: A): B = f(a)
    override def toString = desc
  }

  def JList[A](items: A*): JList[A] = {
    val list = new java.util.ArrayList[A]
    items.foreach(list.add)
    list
  }

  def JMap[K,V](entries: (K,V)*): JMap[K,V] = {
    val map = new java.util.HashMap[K,V]
    entries.foreach {
      case (k,v) => map.put(k,v)
    }
    map
  }

  def JInt(i: Int): JInt = new JInt(i);
}
