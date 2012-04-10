/* Copyright (C) 2008-2010 University of Massachusetts Amherst,
   Department of Computer Science.
   This file is part of "FACTORIE" (Factor graphs, Imperative, Extensible)
   http://factorie.cs.umass.edu, http://code.google.com/p/factorie/
   Licensed under the Apache License, Version 2.0 (the "License");
   you may not use this file except in compliance with the License.
   You may obtain a copy of the License at
    http://www.apache.org/licenses/LICENSE-2.0
   Unless required by applicable law or agreed to in writing, software
   distributed under the License is distributed on an "AS IS" BASIS,
   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
   See the License for the specific language governing permissions and
   limitations under the License. */


package cc.factorie
import cc.factorie._
import cc.factorie.la._
import cc.factorie.util.{DoubleSeq,IntSeq}
import scala.util.Random

// Proportions Values

// TODO Perhaps instead Proportions should contain a Masses, but not inherit from Masses?  (Suggested by Alexandre)  -akm
// But I think this would lead to inefficiencies, and the current set-up isn't bad. -akm
trait Proportions extends Masses {
  abstract override def apply(i:Int): Double = super.apply(i) / massTotal
  def mass(i:Int): Double = super.apply(i)
  override def sampleIndex(implicit r:Random): Int = {
    var b = 0.0; val s = r.nextDouble; var i = 0
    while (b <= s && i < length) { assert (apply(i) >= 0.0); b += apply(i); i += 1 }
    assert(i > 0)
    i - 1
  } 
  //def sampleIndex: Int = sampleIndex(cc.factorie.random)
  @inline final def pr(index:Int) = apply(index)
  @inline final def logpr(index:Int) = math.log(apply(index))
  override def stringPrefix = "Proportions"
  override def toString = this.toSeq.take(10).mkString(stringPrefix+"(", ",", ")")

  // Consider moving this to Tensor (and renaming appropriately) -akm
  class DiscretePr(val index:Int, val pr:Double)
  class DiscretePrSeq(val maxLength:Int) extends Seq[DiscretePr] {
    def this(maxLength:Int, contents:Seq[Double]) = { this(maxLength); var i = 0; while (i < contents.length) { this += (i, contents(i)); i += 1 } }
    private val _seq = new Array[DiscretePr](maxLength)
    private var _length: Int = 0
    def length = _length
    def apply(i:Int) = _seq(i)
    def iterator: Iterator[DiscretePr] = new Iterator[DiscretePr] {
      var i = 0
      def hasNext = i < _length
      def next = { i += 1; _seq(i-1) }
    }
    def +=(index:Int, pr:Double): Unit = {
      if (_length < maxLength || (pr > _seq(_length-1).pr && pr > 0.0)) {
       if (_length < maxLength) { _seq(_length) = new DiscretePr(index, pr); _length += 1 }
       else if (pr > _seq(_length-1).pr) _seq(_length-1) = new DiscretePr(index, pr)
       var i = _length - 1
       while (i > 0 && _seq(i).pr > _seq(i-1).pr) {
         val tmp = _seq(i)
         _seq(i) = _seq(i-1)
         _seq(i-1) = tmp
         i -= 1
       }
      }
    }
  }
  def top(n:Int): Seq[DiscretePr] = new DiscretePrSeq(n, this.toSeq)
}

trait Proportions1 extends Masses1 with Proportions
trait Proportions2 extends Masses2 with Proportions
trait Proportions3 extends Masses3 with Proportions
trait Proportions4 extends Masses4 with Proportions

// Proportions Values of dimensionality 1

class SingletonProportions1(dim1:Int, singleIndex:Int) extends SingletonMasses1(dim1, singleIndex, 1.0) with Proportions1 {
  @inline final override def apply(index:Int) = if (index == singleIndex) 1.0 else 0.0
}
class UniformProportions1(dim1:Int) extends UniformMasses1(dim1, 1.0) with Proportions1 {
  @inline override final def apply(i:Int): Double = 1.0 / dim1
}
class GrowableUniformProportions1(sizeProxy:Iterable[Any], uniformValue:Double = 1.0) extends GrowableUniformMasses1(sizeProxy, uniformValue) with Proportions1 {
  @inline final override def apply(index:Int) = {
    val result = 1.0 / length
    assert(result > 0 && result != Double.PositiveInfinity, "GrowableUniformProportions domain size is negative or zero.")
    result
  }
}

class DenseProportions1(override val dim1:Int) extends DenseMasses1(dim1) with Proportions1 {
  def this(ds:DoubleSeq) = { this(ds.length); this += ds }
  def this(a:Array[Double]) = { this(a.length); this += a }
}
class DenseProportions2(override val dim1:Int, override val dim2:Int) extends DenseMasses2(dim1, dim2) with Proportions2
class DenseProportions3(override val dim1:Int, override val dim2:Int, override val dim3:Int) extends DenseMasses3(dim1, dim2, dim3) with Proportions3
class DenseProportions4(override val dim1:Int, override val dim2:Int, override val dim3:Int, override val dim4:Int) extends DenseMasses4(dim1, dim2, dim3, dim4) with Proportions4

class GrowableDenseProportions1(sizeProxy:Iterable[Any]) extends GrowableDenseMasses1(sizeProxy) with Proportions

class SortedSparseCountsProportions1(dim1:Int) extends SortedSparseCountsMasses1(dim1) with Proportions1 {
  // TODO We need somehow to say that this isDeterministic function of this.prior.
  var prior: Masses = null
  
  // TODO Fix this by implementing a SortedSparseCountsMasses1
  override def apply(index:Int): Double = {
    if (prior eq null) {
      if (countsTotal == 0) 1.0 / length
      else countOfIndex(index).toDouble / countsTotal
    } else {
      if (countsTotal == 0) prior(index) / prior.massTotal
      else countOfIndex(index).toDouble / countsTotal
    }
  }
  // Note that "def zero()" defined in SortedSparseCountsMasses1 does not zero this.prior
  override def top(n:Int): Seq[DiscretePr] =
    for (i <- 0 until math.min(n, numPositions)) yield 
      new DiscretePr(indexAtPosition(i), countAtPosition(i).toDouble / countsTotal)

}



// Proportions Variable

trait ProportionsVar extends MassesVar with VarAndValueType[ProportionsVar,Proportions] {
  // Just a few short-cuts that reach into the value, for the most common operations, already defined in TensorVar
  //final def size = tensor.size
}
class ProportionsVariable extends MassesVariable with ProportionsVar {
  def this(initialValue:Proportions) = { this(); _set(initialValue) }
}
// In the future, we might also need a ProportionsVar1, ProportionsVar2, etc. -akm

object ProportionsVariable {
  def uniform(dim:Int) = new ProportionsVariable(new UniformProportions1(dim))
  def dense(dim:Int) = new ProportionsVariable(new DenseProportions1(dim))
  def growableDense(sizeProxy:Iterable[Any]) = new ProportionsVariable(new GrowableDenseProportions1(sizeProxy))
  def growableUniform(sizeProxy:Iterable[Any]) = new ProportionsVariable(new GrowableUniformProportions1(sizeProxy, 1.0))
  def sortedSparseCounts(dim:Int) = new ProportionsVariable(new SortedSparseCountsProportions1(dim))
  //def growableSparseCounts(sizeProxy:Iterable[Any]) = new ProportionsVariable(new SortedSparseCountsProportions1(sizeProxy)) // Not yet implemented
}


object MaximizeProportions extends Maximize[ProportionsVariable,Nothing] {
  import cc.factorie.generative.{GenerativeModel,Dirichlet,Discrete,Mixture,DiscreteMixture,PlatedDiscreteMixture}
  override def attempt(variables:Iterable[Variable], varying:Iterable[Variable], model:Model, qModel:Model): Boolean = {
    if (varying.size != 0) return false
    if (variables.size != 1) return false
    variables.head match {
      case mp: ProportionsVariable => {
        model match {
          case model:GenerativeModel => apply(mp, model, qModel)
          case _ => return false
        }
      }
      case _ => return false
    }
    true
  }
  def apply(variables:Iterable[ProportionsVariable], varying:Iterable[Nothing], model:Model, qModel:Model): Unit = {
    if (variables.size != 1) throw new Error
    apply(variables.head, model.asInstanceOf[GenerativeModel], qModel)
  }
  //def apply(p:MutableProportions, model:GenerativeModel, qModel:Model = null): Unit = if (apply(p, model.factors(Seq(p)), qModel) == false) throw new Error("Not handled.")
  def apply(p:ProportionsVariable, model:GenerativeModel, qModel:Model): Unit = {
    // Zero an accumulator
    var e: DenseProportions1 = null
    p match {
      case p:DenseProportions1 => { e = p; e.zero() }
      case _ => e = new DenseProportions1(p.tensor.length)
    }
    // Initialize with prior; find the factor that is the parent of "p", and use its Dirichlet masses for initialization
    model.parentFactor(p) match {
      //case f:Dirichlet.Factor => e := f._2.tensor // TODO Uncomment this once Masses have been converted
      case _ => throw new Error
    }
    //factors.collectFirst({case f:Dirichlet.Factor if (f.child == p) => e.set(f._2)(null)})
    // Incorporate children
    @inline def incrementForDiscreteVar(dv:DiscreteVar, incr:Double): Unit = {
      val qFactors = if (qModel eq null) Nil else qModel.factors(Seq(dv))
      if (qFactors.size == 0)
        e.+=(dv.intValue, 1.0)
      else qFactors.head match {
        // The child has a Q distribution
        //case qd:Discrete.Factor => e += qd._2.tensor // forIndex(qd._2.length)(i => e.increment(i, qd._2(i))(null)) // TODO Uncomment this one Proportions have been converted
        case _ => throw new Error
      } // TODO We should check that qFactors.size == 1
    }
    for (factor <- model.extendedChildFactors(p)) factor match {
      //case parent:GenerativeFactor if (parent.child == p) => {} // Parent factor of the Proportions we are estimating already incorporated above
      // The array holding the mixture components; the individual components (DiscreteMixture.Factors) will also be among the extendedChildFactors
      case m:Mixture.Factor => {}
      // A simple DiscreteVar child of the Proportions
      case d:Discrete.Factor => incrementForDiscreteVar(d._1, 1.0)
      // A DiscreteVar child of a Mixture[Proportions]
      case dm:DiscreteMixture.Factor => {
        val gate = dm._2
        val qFactors = if (qModel eq null) Nil else qModel.factors(Seq(gate))
        val mixtureIndex = dm._2.indexOf(e) // Yipes!  Linear search.
        if (qFactors.size == 0) {
          if (dm._3.intValue == mixtureIndex) incrementForDiscreteVar(dm._1, 1.0)
        } else qFactors.head match {
          // The gate has a Q distribution
          case qd:Discrete.Factor => incrementForDiscreteVar(dm._1, qd._2.tensor(mixtureIndex))
        }
      }
      case pdm:PlatedDiscreteMixture.Factor => {
        require(qModel.factors(pdm.variables).size == 0) // We don't yet handle variational qModel of PlatedDiscreteMixture.Factor
        val mixtureIndex = pdm._2.indexOf(e)
        var i = 0; val len = pdm._1.length
        while (i < len) { if (pdm._3.intValue(i) == mixtureIndex) e.+=(pdm._3.intValue(i), 1.0); i += 1 }
      }
    }
    if (p ne e) p.set(e)(null)
  }
}