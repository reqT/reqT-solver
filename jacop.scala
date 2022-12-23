//> using lib "org.jacop:jacop:4.9.0"
package reqt

import org.jacop.{constraints => jcon, core => jcore, search => jsearch}

/**  http://www.jacop.eu/  */

trait Solutions {
  def nVariables: Int
  def variables: Array[Var]
  def varMap: Map[String, Var]
  def indexOf: Map[Var, Int]
  def nSolutions: Int
  def lastSolution: Map[Var, Int]
  def solution(solutionIndex: Int): Array[Int]
  def solutionMap(solutionIndex: Int): Map[Var, Int]
  def value(solutionIndex: Int, variableIndex: Int): Int
  def valueVector(v: Var): Vector[Int]
  def coreDomains: Array[Array[jcore.Domain]]
  def coreVariables: Array[? <: jcore.Var]
  def domain(solutionIndex: Int, variableIndex: Int): jcore.Domain
  def solutionMatrix: Array[Array[Int]]
  def printSolutions: Unit
}

case class Search(
      searchType: SearchType = jacop.Settings.defaultSearchType,
      timeOutOption: Option[Long] = None,
      solutionLimitOption: Option[Int] = None,
      valueSelection: jacop.ValueSelection = jacop.Settings.defaultValueSelection,
      variableSelection: jacop.VariableSelection = jacop.Settings.defaultVariableSelection,
      assignOption: Option[Seq[Var]] = None)

// case class ConstraintProblem(cs: Seq[Constr]) {
//   //Any propagates because of Map invariance in first Type argument:
//   def mkModel(vmap: Map[Var, Int]): Model = { 
//     var newModel = Model()
//     val attrVals = vmap collect { 
//       case (Var(ar @ AttrRef(_,t)), i) if t.isInt => ar(i)  
//     }
//     val otherVariableValues = vmap.toVector flatMap { (vi: (Var,Int)) => vi match {
//       case (Var(x), _) if !x.isInstanceOf[AttrRef[_]] => Some(vi._1 === vi._2)
//       case  _ => None
//     } } 
    
//     attrVals foreach { av => newModel += av  }
//     if (otherVariableValues.isEmpty) newModel
//     else newModel + Constraints(otherVariableValues)
//   }

//   def solve(): (Model, Result) = solve(Search())
//   def solve(search: Search): (Model, Result) =  {
//     val r = jacop.Solver(cs, search).solve
//     if (r.conclusion == SolutionFound) (mkModel(r.lastSolution), r) 
//     else (Model(), r) 
//   }
//   private def searchModelAndWarnIfNoSolution(search: Search): Model = {
//     val (model, result) = solve(search)
//     if (result.conclusion != SolutionFound)
//       jacop.Settings.warningPrinter(result.conclusion.toString)
//     model
//   }
//   def satisfy: Model          = searchModelAndWarnIfNoSolution(Search(Satisfy))
//   def maximize(v: Var): Model = searchModelAndWarnIfNoSolution(Search(Maximize(v)))
//   def minimize(v: Var): Model = searchModelAndWarnIfNoSolution(Search(Minimize(v)))
// }

extension (cs: Seq[Constr]) 
  def solve(search: Search = Search()) = 
    val result = jacop.Solver(cs, search).solve
    if result.conclusion != SolutionFound then
      jacop.Settings.warningPrinter(result.conclusion.toString)
    result
  //if (r.conclusion == SolutionFound) (r.lastSolution, r) else (Map(), r) 

class JacopSolutions( 
        val coreDomains: Array[Array[jcore.Domain]], 
        val coreVariables: Array[? <: jcore.Var],
        val nSolutions: Int,
        val lastSolution: Map[Var, Int]) extends Solutions {
  lazy val nVariables = coreVariables.length
  lazy val varMap: Map[String, Var] = lastSolution.keys.toSeq.map(v => (v.ref.toString, v)).toMap
  lazy val variables: Array[Var] = coreVariables.map(v => varMap(v.id))
  lazy val indexOf: Map[Var, Int] = variables.zipWithIndex.toMap
  private def toInt(d: jcore.Domain): Int = d.asInstanceOf[jcore.IntDomain].value
  def domain(solutionIndex: Int, variableIndex: Int): jcore.Domain = coreDomains(solutionIndex)(variableIndex)
  def value(s: Int, v: Int): Int = toInt(domain(s,v))
  def solution(s: Int): Array[Int] = coreDomains(s).map(toInt)
  def solutionMap(s: Int): Map[Var, Int] = 
    ( for i <- 0 until nVariables yield (variables(i), value(s, i)) ) .toMap
  def valueVector(v: Var): Vector[Int] = 
    ( for s <- 0 until nSolutions yield value(s, indexOf(v)) ) .toVector   
  def printSolutions: Unit = for i <- 0 until nSolutions do println(s"*** Solution $i:\n" + solutionMap(i))
  lazy val solutionMatrix: Array[Array[Int]] = coreDomains.slice(0,nSolutions-1).map(_.map(toInt))
  override def toString = s"Solutions([nSolutions=$nSolutions][nVariables=$nVariables])" 
}



object jacop {  

  case class Config(
      val defaultInterval: Interval = Interval(-1000, 1000),
      val defaultSearchType: SearchType = Satisfy,
      val defaultValueSelection: ValueSelection = IndomainRandom,
      val defaultVariableSelection: VariableSelection = InputOrder,
      val verbose: Boolean = false,
      val debug: Boolean = false,
      val warningPrinter: String => Unit = (s: String) => println("WARNING: " + s),
  )
  object Config:
    given defaultConfig: Config = Config()


  object Settings {
    var defaultInterval: Interval = Interval(-1000, 1000)
    var defaultSearchType: SearchType = Satisfy
    var defaultValueSelection: ValueSelection = IndomainRandom
    var defaultVariableSelection: VariableSelection = InputOrder
    var verbose: Boolean = false
    var debug: Boolean = false
    var warningPrinter: String => Unit = (s: String) => println("WARNING: " + s)
  }

  type JIntVar = jcore.IntVar
  type JVar = jcore.Var
  type JBooleanVar = jcore.BooleanVar
  
  sealed trait ValueSelection { def toJacop: jsearch.Indomain[JIntVar] }
  case object IndomainMax  extends ValueSelection { def toJacop = new jsearch.IndomainMax[JIntVar] }
  case object IndomainMedian  extends ValueSelection { def toJacop = new jsearch.IndomainMedian[JIntVar] }
  case object IndomainMiddle  extends ValueSelection { def toJacop = new jsearch.IndomainMiddle[JIntVar] }
  case object IndomainMin  extends ValueSelection { def toJacop = new jsearch.IndomainMin[JIntVar] }
  case object IndomainRandom  extends ValueSelection { def toJacop = new jsearch.IndomainRandom[JIntVar] }
  case object IndomainSimpleRandom  extends ValueSelection { def toJacop = new jsearch.IndomainSimpleRandom[JIntVar] }

/* 
value selection methods not yet implemented    
  case object IndomainSetMax  extends ValueSelection { def toJacop = new jsearch.IndomainMax[JSetVar] }
  case object IndomainSetMin  extends ValueSelection { def toJacop = new jsearch.IndomainMax[JSetVar] }
  case object IndomainSetRandom  extends ValueSelection { def toJacop = new jsearch.IndomainMax[JSetVar] }
  case object IndomainHierarchical  extends ValueSelection 
  case object IndomainList  extends ValueSelection  
*/
  sealed trait VariableSelection { 
    def toJacop(s: jcore.Store, jvs: Array[JIntVar], valSelect: ValueSelection): 
      jsearch.SelectChoicePoint[JIntVar] 
  }
  case object InputOrder extends VariableSelection { 
    def toJacop(s: jcore.Store, jvs: Array[JIntVar], valSelect: ValueSelection) =
        new jsearch.InputOrderSelect[JIntVar](s, jvs, valSelect.toJacop) 
  }
  case object LargestDomain extends VariableSelection { 
    def toJacop(s: jcore.Store, jvs: Array[JIntVar], valSelect: ValueSelection) =
        new jsearch.SimpleSelect[JIntVar](jvs, new jsearch.LargestDomain(), valSelect.toJacop) 
  }
  case object SmallestDomain extends VariableSelection { 
    def toJacop(s: jcore.Store, jvs: Array[JIntVar], valSelect: ValueSelection) =
        new jsearch.SimpleSelect[JIntVar](jvs, new jsearch.SmallestDomain(), valSelect.toJacop) 
  }
  case object LargestMax extends VariableSelection { 
    def toJacop(s: jcore.Store, jvs: Array[JIntVar], valSelect: ValueSelection) =
        new jsearch.SimpleSelect[JIntVar](jvs, new jsearch.LargestMax(), valSelect.toJacop) 
  }
  case object LargestMin extends VariableSelection { 
    def toJacop(s: jcore.Store, jvs: Array[JIntVar], valSelect: ValueSelection) =
        new jsearch.SimpleSelect[JIntVar](jvs, new jsearch.LargestMin(), valSelect.toJacop) 
  }
  case object SmallestMax extends VariableSelection { 
    def toJacop(s: jcore.Store, jvs: Array[JIntVar], valSelect: ValueSelection) =
        new jsearch.SimpleSelect[JIntVar](jvs, new jsearch.SmallestMax(), valSelect.toJacop) 
  }
  case object SmallestMin extends VariableSelection { 
    def toJacop(s: jcore.Store, jvs: Array[JIntVar], valSelect: ValueSelection) =
        new jsearch.SimpleSelect[JIntVar](jvs, new jsearch.SmallestMin(), valSelect.toJacop) 
  }
  case object MaxRegret extends VariableSelection { 
    def toJacop(s: jcore.Store, jvs: Array[JIntVar], valSelect: ValueSelection) =
        new jsearch.SimpleSelect[JIntVar](jvs, new jsearch.MaxRegret(), valSelect.toJacop) 
  }
  case object MostConstrainedDynamic extends VariableSelection { 
    def toJacop(s: jcore.Store, jvs: Array[JIntVar], valSelect: ValueSelection) =
        new jsearch.SimpleSelect[JIntVar](jvs, new jsearch.MostConstrainedDynamic(), valSelect.toJacop) 
  }
  case object MostConstrainedStatic extends VariableSelection { 
    def toJacop(s: jcore.Store, jvs: Array[JIntVar], valSelect: ValueSelection) =
        new jsearch.SimpleSelect[JIntVar](jvs, new jsearch.MostConstrainedStatic(), valSelect.toJacop) 
  }
  
  /* variable selection methods not yet implemented:
    MaxCardDiff    
    MaxGlbCard    
    MaxLubCard    
    MinCardDiff    
    MinDomainOverDegree    
    MinGlbCard    
    MinLubCard    
    WeightedDegree    
  */

  object SolverUtils {
    type Ivls = Map[Var, Seq[Interval]]
    def distinctVars(cs: Seq[Constr]): Seq[Var] = cs.map { _.variables } .flatten.distinct
    def collectBounds(cs: Seq[Constr]): Seq[Bounds] = cs collect { case b: Bounds => b }
    def collectConstr(cs: Seq[Constr]): Seq[Constr] = cs filter { case b: BoundingConstr => false ; case _ => true }
    def intervals(b: Bounds): Map[Var, Seq[Interval]] = b.variables.map(v => (v, b.domain)).toMap
    def mergeIntervals(ivls1: Ivls, ivls2: Ivls): Ivls = {
      var result = ivls1
      for (v, ivls) <- ivls2 do { 
        if result.isDefinedAt(v) then result += v -> (result(v) ++ ivls) 
        else result += v -> ivls 
      }
      result
    }
    def buildDomainMap(cs: Seq[Constr]): Ivls = {
      var result = collectBounds(cs).map(intervals(_)).foldLeft(Map(): Ivls)(mergeIntervals(_,_))
      for v <- distinctVars(cs) do {
        if !result.isDefinedAt(v) then result += v -> Seq(jacop.Settings.defaultInterval)
      }
      result
    }
    def nameToVarMap(vs: Seq[Var]): Map[String, Var] = vs.map(v => (v.ref.toString, v)).toMap
    def checkUniqueToString(vs: Seq[Var]): Set[String] = {
      val strings = vs.map(_.ref.toString)
      strings.diff(strings.distinct).toSet
    }
    def checkIfNameExists(name: String, vs: Seq[Var]): Boolean = 
      vs.exists { case Var(ref) => ref.toString == name }
    
    def flattenAllConstraints(cs: Seq[Constr]): Seq[Constr] = {
      def flatten(xs: Seq[Constr]): Seq[Constr] = 
        if xs.isEmpty then xs 
        else (xs.head match {
          //case cs: Constraints => flatten(cs.value)  ???
          case c => Seq(c)
        } ) ++ flatten(xs.tail)
      flatten(cs)
    }
  }         
  
  case class Solver(constraints: Seq[Constr], search: Search)  {
    
    import search.*

    import SolverUtils.*
    
    def toJCon(constr: Constr, store: jcore.Store, jIntVar: Map[Var, JIntVar]): jcon.Constraint = {
      def jVarArray(vs: Seq[Var]) = vs.map(v => jIntVar(v)).toArray
      constr match {
        case AbsXeqY(x, y) => jcon.AbsXeqY(jIntVar(x), jIntVar(y))
        case AllDifferent(vs) => jcon.Alldiff(jVarArray(vs))
        case And(cs) => 
          jcon.And(cs.map(c => 
              toJCon(c, store, jIntVar).asInstanceOf[jcon.PrimitiveConstraint]
            ).toArray
          )
        case IndexValue(ix, vs, v) => jcon.Element.choose(jIntVar(ix), jVarArray(vs), jIntVar(v))
        case SumEq(vs, x)          => jcon.SumInt(vs.map(v => jIntVar(v)).toArray, "==", jIntVar(x))
        case Count(vs, x, c)       => jcon.Count(vs.map(v => jIntVar(v)).toArray, jIntVar(x),  c)
        case XeqC(x, c)            => jcon.XeqC(jIntVar(x), c)
        case XeqY(x, y)            => jcon.XeqY(jIntVar(x), jIntVar(y))
        case XdivYeqZ(x, y, z)     => jcon.XdivYeqZ(jIntVar(x), jIntVar(y), jIntVar(z))
        case XexpYeqZ(x, y, z)     => jcon.XexpYeqZ(jIntVar(x), jIntVar(y), jIntVar(z))
        case XmulYeqZ(x, y, z)     => jcon.XmulYeqZ(jIntVar(x), jIntVar(y), jIntVar(z))
        case XplusYeqZ(x, y, z)    => jcon.XplusYeqZ(jIntVar(x), jIntVar(y), jIntVar(z))
        case XplusYlteqZ(x, y, z)  => jcon.XplusYlteqZ(jIntVar(x), jIntVar(y), jIntVar(z))
        case Distance(x, y, z)     => jcon.Distance(jIntVar(x), jIntVar(y), jIntVar(z))
        case XgtC(x, c)     => jcon.XgtC(jIntVar(x), c)
        case XgteqC(x, c)   => jcon.XgteqC(jIntVar(x), c)
        case XgteqY(x, y)   => jcon.XgteqY(jIntVar(x), jIntVar(y))
        case XgtY(x, y)     => jcon.XgtY(jIntVar(x), jIntVar(y))
        case XltC(x, c)     => jcon.XltC(jIntVar(x), c)
        case XlteqC(x, c)   => jcon.XlteqC(jIntVar(x), c)
        case XlteqY(x, y)   => jcon.XlteqY(jIntVar(x), jIntVar(y))
        case XltY(x, y)     => jcon.XltY(jIntVar(x), jIntVar(y))
        case XneqC(x, c)    => jcon.XneqC(jIntVar(x), c)
        case XneqY(x, y)    => jcon.XneqY(jIntVar(x), jIntVar(y))
        case XeqBool(x, b)  => jcon.XeqC(jIntVar(x), if b then 1 else 0)
        case IfThen(c1, c2) =>
          val jc = (toJCon(c1, store, jIntVar), toJCon(c2, store, jIntVar)) 
          jcon.IfThen(jc._1.asInstanceOf[jcon.PrimitiveConstraint],   jc._2.asInstanceOf[jcon.PrimitiveConstraint])
        case IfThenElse(c1, c2, c3) =>
          val vs = Vector(c1, c2, c3)
          val jc = vs.map(toJCon(_, store, jIntVar).asInstanceOf[jcon.PrimitiveConstraint]) 
          jcon.IfThenElse(jc(0), jc(1), jc(2))
        case IfThenBool(x,y,z) => jcon.IfThenBool(jIntVar(x), jIntVar(y), jIntVar(z))
        case Reified(c1, x) =>
          val jc = toJCon(c1, store, jIntVar).asInstanceOf[jcon.PrimitiveConstraint]
          jcon.Reified(jc, jIntVar(x))
        case Diff2(rectangles) => 
          def matrix: Array[Array[JIntVar]] = rectangles.map(jVarArray(_)).toArray
          jcon.Diff2(matrix)
        case Binpacking(item, load, size) =>
          jcon.binpacking.Binpacking(jVarArray(item), jVarArray(load), size.toArray)
        case c => println("Constr to JaCoP match error: " + c); ???
      }
    }
    
    lazy val flatConstr = flattenAllConstraints(constraints)
    lazy val domainOf: Map[Var, Seq[Interval]] = buildDomainMap(flatConstr)
 
    def varToJIntVar(v: Var, s: jcore.Store): JIntVar = {
      val intDom = new jcore.IntervalDomain()
      domainOf(v).foreach(ivl => intDom.addDom( new jcore.IntervalDomain(ivl.min, ivl.max)))
      new JIntVar(s, v.ref.toString, intDom) 
    }

    val minimizeHelpVarName = "_$minimize0"

    def collectIntVars(s: jcore.Store): Array[JIntVar] = 
      s.vars.collect { case x: JIntVar if (x.id != minimizeHelpVarName) => x } .toArray
            
    def solutionMap(s: jcore.Store, nameToVar: Map[String, Var] ): Map[Var, Int] = 
          collectIntVars(s).filter(_.singleton).map(iv => (nameToVar(iv.id), iv.value) ).toMap

    def solve: Result = if constraints.size > 0 then {
      val store = new jcore.Store
      val vs = distinctVars(flatConstr)
      val cs = collectConstr(flatConstr)
      if Settings.verbose then println("*** VARIABLES:   " + vs.mkString(","))
      if Settings.verbose then println("*** CONSTRAINTS: " + cs.mkString(","))
      
      val duplicates = checkUniqueToString(vs)

      if !duplicates.isEmpty then 
        return Result(SearchFailed("Duplicate toString values of variables:" + 
          duplicates.mkString(", "))        )

      if checkIfNameExists(minimizeHelpVarName, vs) then 
        return Result(SearchFailed("Reserved variable name not allowed:" + minimizeHelpVarName))

      val intVarMap: Map[Var, JIntVar] = vs.map { v => (v, varToJIntVar(v, store)) } .toMap

      searchType match
        case opt: Optimize if (!intVarMap.isDefinedAt(opt.cost)) =>
          Result(SearchFailed("Cost variable not defined:" + opt.cost))
          
        case _ => 
          cs foreach { c => store.impose(toJCon(c, store, intVarMap)) }
          if Settings.debug then println(store)
          if !store.consistency then return Result(InconsistencyFound)
          val label = new jsearch.DepthFirstSearch[JIntVar]
          
          label.setPrintInfo(Settings.verbose) 
          
          def listener = label.getSolutionListener().asInstanceOf[jsearch.SimpleSolutionListener[JIntVar]]

          def setup(searchAll: Boolean, recordSolutions: Boolean): Unit =  {
            //this must be done in the right order as JaCoP may overwrite solutionLimit if
            // searchAll is set after setting solutionLimit as explained by Kris
            listener.searchAll(searchAll)
            listener.recordSolutions(recordSolutions)
            timeOutOption.map { timeOut => label.setTimeOut(timeOut) } 
            solutionLimitOption.map { limit =>  listener.setSolutionLimit(limit) }
          }
          
          val variablesToAssign: Array[JIntVar] = 
            if !assignOption.isDefined then collectIntVars(store) //assign all in store
            else assignOption.get.map(intVarMap(_)).toArray
          val selectChoicePoint = variableSelection.toJacop(store, variablesToAssign, valueSelection)
          def solutionNotFound = Result(SolutionNotFound)
          def solutionInStore = solutionMap(store, nameToVarMap(vs))
          def interruptOpt: Option[SearchInterrupt] = 
            if label.timeOutOccured then Some(SearchTimeOut) 
            else if listener.solutionLimitReached && solutionLimitOption.isDefined then 
              Some(SolutionLimitReached)
            else None
          def oneResult(ok: Boolean) = 
            if ok then Result(SolutionFound, 1, solutionInStore, interruptOpt)  
            else solutionNotFound
          def countResult(ok: Boolean, i: Int) = 
            if ok then Result(SolutionFound, i, solutionInStore, interruptOpt) 
            else solutionNotFound
          val conclusion = searchType match {
            case Satisfy => 
              setup(searchAll = false , recordSolutions = true )
              oneResult(label.labeling(store, selectChoicePoint)) 
            case CountAll => //count solutions but don't record any solution to save memory
              setup(searchAll = true , recordSolutions = false )
              countResult(label.labeling(store, selectChoicePoint), listener.solutionsNo) 
            case FindAll => 
              setup(searchAll = true , recordSolutions = true )
              val found = label.labeling(store, selectChoicePoint)
              if !found then solutionNotFound else {
                val solutions: Solutions = JacopSolutions(
                      listener.getSolutions(), 
                      listener.getVariables(), 
                      listener.solutionsNo(), 
                      solutionInStore)
                Result(SolutionFound, solutions.nSolutions, solutionInStore, interruptOpt, Some(solutions))
              }
            case minimize: Minimize => 
              setup(searchAll = false , recordSolutions = true )
              oneResult(label.labeling(store, selectChoicePoint, intVarMap(minimize.cost)))
            case m: Maximize =>  
              setup(searchAll = false , recordSolutions = true )
              val intDom = new jcore.IntervalDomain()
              domainOf(m.cost) foreach (ivl => intDom.addDom( new jcore.IntervalDomain(-ivl.max, -ivl.min)))
              val negCost = new JIntVar(store, minimizeHelpVarName, intDom)
              store.impose( new jcon.XmulCeqZ(intVarMap(m.cost), -1, negCost) )
              oneResult(label.labeling(store, selectChoicePoint, negCost))
          }
          if Settings.verbose then println(store)
          conclusion
    } else Result(SearchFailed("Empty constraints in argument to solve")) //end def solve
    
  } //end class Solver
} // end object jacop

