///////////////////////////////////////////////////////////////////////////////
//        copyright (c) 2012-2018 Oscar Riveros. all rights reserved.        //
//                           oscar.riveros@peqnp.com                         //
//                                                                           //
//   without any restriction, Oscar Riveros reserved rights, patents and     //
//  commercialization of this knowledge or derived directly from this work.  //
///////////////////////////////////////////////////////////////////////////////

package science.peqnp.satisfiability;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.io.Serializable;
import java.util.Comparator;
import java.util.Random;

public class Solver {

    private static final boolean RND_INIT_ACT_DEFAULT = true;
    private static final boolean BIAS_RESTART_DEFAULT = true;

    private static final int PHASE_SAVING_DEFAULT = 2;
    private static final int RESTART_FIRST_DEFAULT = 1000;

    private static final double VAR_DECAY_DEFAULT = 0.95f;
    private static final double CLAUSE_DECAY_DEFAULT = 0.999f;
    private static final double RANDOM_VAR_FREQ_DEFAULT = 0;
    private static final double RESTART_INC_DEFAULT = 1;

    private Vector<Ternary> assigns = new Vector<>();
    private Vector<Boolean> polarity = new Vector<>();
    private Vector<Boolean> decision = new Vector<>();
    private Vector<Literal> trail = new Vector<>();
    private Vector<Integer> trailLimit = new Vector<>();
    private Vector<Block> block = new Vector<>();
    private Vector<Literal> learntClause = new Vector<>();
    private Vector<Ternary> model = new Vector<>();
    private Vector<Literal> conflict = new Vector<>();
    private Vector<Clause> clauses = new Vector<>();
    private Vector<Clause> learnt = new Vector<>();
    private Vector<Double> activity = new Vector<>();
    private Vector<Literal> assumptions = new Vector<>();
    private Vector<Boolean> seen = new Vector<>();
    private Vector<Literal> analyzeStack = new Vector<>();
    private Vector<Literal> analyzeToClear = new Vector<>();
    private String cnfFile;
    private Watchers watches = new Watchers();
    private Heap orderedHeap = new Heap(new Heap.Order(activity));
    private ReduceDB reduceDB = new ReduceDB();
    private Random random = new Random();
    private boolean biasRestart;
    private boolean randomPool;
    private boolean randomInitActivity;
    private boolean ok;
    private boolean asyncInterrupt;
    private boolean removeSatisfied;
    private int conflictCounter;
    private int phaseSaving;
    private int restartFirst;
    private int learntSizeAdjustStartConflict;
    private int head;
    private int simplifyDBAssigns;
    private int learntSizeAdjustCnt;
    private int clausesLiterals;
    private int learntLiterals;
    private int simplifyDBPropositions;
    private int conflictBudget;
    private int propagationBudget;
    private int propagation;
    private int conflicts;
    private int cursor;
    private double maxLearnt;
    private double learntSizeAdjustConflict;
    private double learntSizeAdjustIncrement;
    private double learntSizeIncrement;
    private double variableDecay;
    private double clauseDecay;
    private double randomVariableFrequency;
    private double restartIncrement;
    private double clauseDecayIncrement;
    private double variableActivityIncrement;

    public Solver() {
        cursor = Integer.MAX_VALUE;
    }

    private static double activation(double a, double x) {
        if (a < 0) {
            return (1.0 / (1 - a * (a + x)));
        } else {
            return Math.exp(a * x);
        }
    }

    private void loadDefault() {
        ok = true;
        randomPool = false;
        removeSatisfied = true;
        asyncInterrupt = false;

        variableDecay = VAR_DECAY_DEFAULT;
        clauseDecay = CLAUSE_DECAY_DEFAULT;
        randomVariableFrequency = RANDOM_VAR_FREQ_DEFAULT;
        biasRestart = BIAS_RESTART_DEFAULT;
        phaseSaving = PHASE_SAVING_DEFAULT;
        randomInitActivity = RND_INIT_ACT_DEFAULT;
        restartFirst = RESTART_FIRST_DEFAULT;
        restartIncrement = RESTART_INC_DEFAULT;

        clauseDecayIncrement = 1;
        variableActivityIncrement = 1;
        head = 0;
        simplifyDBAssigns = 0;
        simplifyDBPropositions = 0;
        conflictBudget = -1;
        propagationBudget = -1;
        learntSizeIncrement = 1.5f;
        learntSizeAdjustIncrement = 1.5f;
        learntSizeAdjustStartConflict = 100;
    }

    private Clause reason(Variable variable) {
        return block.get(variable.value()).reason;
    }

    private int level(int backtrackLevel) {
        return block.get(backtrackLevel).level;
    }

    private void insertVariableOrdered(Variable variable) {
        if (!orderedHeap.inHeap(variable.value()) && decision.get(variable.value())) {
            orderedHeap.insert(variable.value());
        }
    }

    private void variableDecayActivity() {
        variableActivityIncrement *= Math.sinh(variableDecay);
    }

    private void variableBumpActivity(Variable variable) {
        variableBumpActivity(variable, variableActivityIncrement);
    }

    private void variableBumpActivity(Variable variable, double increment) {
        int i = variable.value();
        if (activity.set(i, activity.get(i) + increment) >= numberOfClauses()) {
            for (int j = 0; j < numberOfVariables(); j++)
                activity.set(j, (activity.get(j) / numberOfVariables()));
            variableActivityIncrement *= 1.0 / numberOfVariables();
        }
        if (orderedHeap.inHeap(i)) {
            orderedHeap.decrease(i);
        }
    }

    private void clauseDecayActivity() {
        clauseDecayIncrement *= Math.sinh(clauseDecay);
    }

    private void clauseBumpActivity(Clause cls) {
        if (cls.activity((cls.activity() + clauseDecayIncrement)) >=
                numberOfClauses()) {
            for (int i = 0; i < learnt.size(); i++) {
                learnt.get(i).activity((learnt.get(i).activity() / numberOfClauses()));
            }
            clauseDecayIncrement *= (1.0 / numberOfClauses());
        }
    }

    private boolean locked(Clause cls) {
        Clause reason = reason(Variable.valueOf(cls.get(0).variable()));
        return value(cls.get(0)) == Ternary.TRUE && reason != null &&
                reason.equals(cls);
    }

    private void newDecisionLevel() {
        trailLimit.push(trail.size());
    }

    private int decisionLevel() {
        return trailLimit.size();
    }

    private int abstractLevel(int position) {
        return 1 << (level(position) & 31);
    }

    private Ternary value(Variable variable) {
        return assigns.get(variable.value());
    }

    private Ternary value(Literal literal) {
        return (assigns.get(literal.variable())).xor(literal.sign());
    }

    private int nAssigns() {
        return trail.size();
    }

    private int numberOfClauses() {
        return clauses.size();
    }

    private int numberOfVariables() {
        return block.size();
    }

    private void setDecisionVariable(Variable variable) {
        int i = variable.value();
        decision.set(i, true);
        insertVariableOrdered(variable);
    }

    private boolean withinBudget() {
        return asyncInterrupt ||
                (conflictBudget >= 0 && conflicts >= conflictBudget) ||
                (propagationBudget >= 0 && propagation >= propagationBudget);
    }

    private void newVariable() {
        int len = numberOfVariables();
        Variable variable = Variable.valueOf(len);

        watches.init(Literal.valueOf(len, true));
        assigns.push(Ternary.UNDEF);
        block.push(null);

        activity.push((randomInitActivity ? Math.sinh(random.nextDouble()) : 0.0));
        seen.push(false);
        polarity.push(true);
        decision.push(true);
        trail.capacity(len + 1);
        setDecisionVariable(variable);
    }

    private void addClause(Vector<Literal> literals) {
        if (!ok)
            return;

        Literal literal;
        int i, j;
        for (i = j = 0, literal = Literal.UNDEF; i < literals.size(); i++) {
            int v = literals.get(i).variable();
            while (v >= numberOfVariables()) {
                newVariable();
            }
            if (value(literals.get(i)) == Ternary.TRUE || literals.get(i) == literal.not()) {
                return;
            } else if (value(literals.get(i)) != Ternary.FALSE && literals.get(i) != literal) {
                literals.set(j++, literal = literals.get(i));
            }
        }
        literals.shrink(i - j);

        if (literals.size() == 0) {
            ok = false;
            return;
        } else if (literals.size() == 1) {
            uncheckedEnqueue(literals.get(0));
            ok = (propagate() == null);
            return;
        } else {
            Clause cr = new Clause(literals, false);
            clauses.push(cr);
            attachClause(cr);
        }
    }

    private void attachClause(Clause cls) {
        watches.get(cls.get(0).not()).push(new Watcher(cls, cls.get(1)));
        watches.get(cls.get(1).not()).push(new Watcher(cls, cls.get(0)));
        if (cls.learnt()) {
            learntLiterals += cls.size();
        } else {
            clausesLiterals += cls.size();
        }
    }

    private void removeClause(Clause clause) {
        clause.mark(1);
    }

    private boolean isSatisfied(Clause cls) {
        for (Literal lit : cls) {
            if (value(lit) == Ternary.TRUE) {
                return true;
            }
        }
        return false;
    }

    private void cancelUntil(int level) {
        if (decisionLevel() > level) {
            for (int c = trail.size() - 1; c >= trailLimit.get(level); c--) {
                int x = trail.get(c).variable();
                assigns.set(x, Ternary.UNDEF);
                if ((phaseSaving > 1) || ((phaseSaving == 1) && c > trailLimit.last())) {
                    polarity.set(x, trail.get(c).sign());
                }
                insertVariableOrdered(Variable.valueOf(x));
            }
            head = trailLimit.get(level);
            trail.shrink(trail.size() - trailLimit.get(level));
            trailLimit.shrink(trailLimit.size() - level);
        }
    }

    private Literal pickBranchLit() {
        Variable next = Variable.UNDEF;
        if (random.nextDouble() < randomVariableFrequency && !orderedHeap.empty()) {
            next = Variable.valueOf(orderedHeap.get(random.nextInt(orderedHeap.size())));
        }

        while (next == Variable.UNDEF || value(next) != Ternary.UNDEF || !decision.get(next.value()))
            if (orderedHeap.empty()) {
                next = Variable.UNDEF;
                break;
            } else {
                next = Variable.valueOf(orderedHeap.removeMin());
            }

        return next == Variable.UNDEF
                ? Literal.UNDEF
                :

                Literal.valueOf(next.value(), randomPool ? random.nextBoolean()
                        : polarity.get(next.value()));
    }

    private int analyze(Clause conflict, Vector<Literal> outLearnt) {
        int pathC = 0;
        Literal p = Literal.UNDEF;

        outLearnt.push(null);
        int index = trail.size() - 1;

        do {
            Clause cls = conflict;

            if (cls.learnt()) {
                clauseBumpActivity(cls);
            }

            for (int j = (p == Literal.UNDEF) ? 0 : 1; j < cls.size(); j++) {
                Literal q = cls.get(j);

                if (!seen.get(q.variable()) && level(q.variable()) > 0) {
                    variableBumpActivity(Variable.valueOf(q.variable()));
                    seen.set(q.variable(), true);
                    if (level(q.variable()) >= decisionLevel()) {
                        pathC++;
                    } else {
                        outLearnt.push(q);
                    }
                }
            }

            while (!seen.get(trail.get(index--).variable())) ;
            p = trail.get(index + 1);
            conflict = reason(Variable.valueOf(p.variable()));
            seen.set(p.variable(), false);
            pathC--;

        } while (pathC > 0);
        outLearnt.set(0, p.not());

        int i, j;
        outLearnt.copyTo(analyzeToClear);

        int abstract_level = 0;
        for (i = 1; i < outLearnt.size(); i++) {
            abstract_level |= abstractLevel(outLearnt.get(i).variable());
        }

        for (i = j = 1; i < outLearnt.size(); i++) {
            if (reason(Variable.valueOf(outLearnt.get(i).variable())) == null || !litRedundant(outLearnt.get(i), abstract_level)) {
                outLearnt.set(j++, outLearnt.get(i));
            }
        }

        outLearnt.shrink(i - j);

        int outBtlevel;
        if (outLearnt.size() == 1)
            outBtlevel = 0;
        else {
            int max_i = 1;

            for (int k = 2; k < outLearnt.size(); k++)
                if (level(outLearnt.get(k).variable()) >
                        level(outLearnt.get(max_i).variable()))
                    max_i = k;

            Literal pp = outLearnt.get(max_i);
            outLearnt.set(max_i, outLearnt.get(1));
            outLearnt.set(1, pp);
            outBtlevel = level(pp.variable());
        }

        for (int k = 0; k < analyzeToClear.size(); k++) {
            seen.set(analyzeToClear.get(k).variable(), false);
        }
        return outBtlevel;
    }

    private boolean litRedundant(Literal p, int abstract_levels) {
        analyzeStack.clear();
        analyzeStack.push(p);
        int top = analyzeToClear.size();
        while (analyzeStack.size() > 0) {
            Clause c = reason(Variable.valueOf(analyzeStack.last().variable()));
            analyzeStack.pop();

            for (int i = 1; i < c.size(); i++) {
                Literal pp = c.get(i);
                if (!seen.get(pp.variable()) && level(pp.variable()) > 0) {
                    if (reason(Variable.valueOf(pp.variable())) != null && (abstractLevel(pp.variable()) & abstract_levels) != 0) {
                        seen.set(pp.variable(), true);
                        analyzeStack.push(pp);
                        analyzeToClear.push(pp);
                    } else {
                        for (int j = top; j < analyzeToClear.size(); j++) {
                            seen.set(analyzeToClear.get(j).variable(), false);
                        }
                        analyzeToClear.shrink(analyzeToClear.size() - top);
                        return false;
                    }
                }
            }
        }

        return true;
    }

    private void analyzeFinal(Literal p, Vector<Literal> outConflict) {
        outConflict.clear();
        outConflict.push(p);

        if (decisionLevel() == 0)
            return;

        seen.set(p.variable(), true);

        for (int i = trail.size() - 1; i >= trailLimit.get(0); i--) {
            Variable x = Variable.valueOf(trail.get(i).variable());
            if (seen.get(x.value())) {
                if (reason(x) == null) {
                    assert (level(x.value()) > 0);
                    outConflict.push(trail.get(i).not());
                } else {
                    Clause c = reason(x);
                    for (int j = 1; j < c.size(); j++)
                        if (level(c.get(j).variable()) > 0)
                            seen.set(c.get(j).variable(), true);
                }
                seen.set(x.value(), false);
            }
        }

        seen.set(p.variable(), false);
    }

    private void uncheckedEnqueue(Literal p, Clause from) {
        assigns.set(p.variable(), Ternary.valueOf(!p.sign()));
        block.set(p.variable(), Block.makeBlock(from, decisionLevel()));
        trail.push(p);
    }

    private void uncheckedEnqueue(Literal p) {
        uncheckedEnqueue(p, null);
    }

    private Clause propagate() {
        Clause confl = null;
        watches.cleanAll();

        int numProps = 0;
        while (head < trail.size()) {
            Literal p = trail.get(head++);
            Vector<Watcher> ws = watches.get(p);
            numProps++;

            int i = 0, j = 0, size = ws.size();
            L:
            while (i < size) {

                Literal blocker = ws.get(i).blocker;
                if (value(blocker) == Ternary.TRUE) {
                    ws.set(j++, ws.get(i++));
                    continue;
                }

                Clause c = ws.get(i).clause;
                Literal false_lit = p.not();
                if (c.get(0) == false_lit) {
                    c.set(0, c.get(1));
                    c.set(1, false_lit);
                }
                assert c.get(1) == false_lit;
                i++;

                Literal first = c.get(0);
                Watcher w = new Watcher(c, first);
                if (first != blocker && value(first) == Ternary.TRUE) {
                    ws.set(j++, w);
                    continue;
                }

                for (int k = 2; k < c.size(); k++) {
                    Literal ck = c.get(k);
                    if (value(ck) != Ternary.FALSE) {
                        c.set(1, ck);
                        c.set(k, false_lit);
                        watches.get(c.get(1).not()).push(w);
                        continue L;
                    }
                }

                ws.set(j++, w);
                if (value(first) == Ternary.FALSE) {
                    confl = c;
                    head = trail.size();

                    while (i < size)
                        ws.set(j++, ws.get(i++));
                } else
                    uncheckedEnqueue(first, c);
            }
            ws.shrink(i - j);
        }
        propagation += numProps;
        simplifyDBPropositions -= numProps;

        return confl;
    }

    private void reduceDB() {
        int i, j;
        double extra_lim = clauseDecayIncrement / learnt.size();

        learnt.sort(reduceDB);

        for (i = j = 0; i < learnt.size(); i++) {
            Clause c = learnt.get(i);
            if (c.size() > 2 && !locked(c) && (i < learnt.size() / 2 || c.activity() < extra_lim)) {
                removeClause(learnt.get(i));
            } else {
                learnt.set(j++, learnt.get(i));
            }
        }
        learnt.shrink(i - j);
    }

    private void removeSatisfied(Vector<Clause> cs) {
        int i, j;
        for (i = j = 0; i < cs.size(); i++) {
            Clause c = cs.get(i);
            if (isSatisfied(c)) {
                removeClause(cs.get(i));
            } else {
                cs.set(j++, cs.get(i));
            }
        }
        cs.shrink(i - j);
    }

    private void rebuildOrderHeap() {
        Vector<Integer> vs = new Vector<>();
        for (int v = 0; v < numberOfVariables(); v++)
            if (decision.get(v) && value(Variable.valueOf(v)) == Ternary.UNDEF) {
                vs.push(v);
            }
        orderedHeap.build(vs);
    }

    private boolean simplify() {
        if (!ok || propagate() != null) {
            ok = false;
            return ok;
        }

        if (nAssigns() == simplifyDBAssigns || (simplifyDBPropositions > 0)) {
            return false;
        }

        removeSatisfied(learnt);
        if (removeSatisfied) {
            removeSatisfied(clauses);
        }
        rebuildOrderHeap();
        simplifyDBAssigns = nAssigns();
        simplifyDBPropositions = clausesLiterals + learntLiterals;

        return false;
    }

    private Ternary search(int nof_conflicts) {
        for (; ; ) {

            Clause clauseConflict = propagate();

            if (clauseConflict != null) {

                conflicts++;
                conflictCounter++;
                if (decisionLevel() == 0)
                    return Ternary.FALSE;

                learntClause.clear();
                int backtrackLevel = analyze(clauseConflict, learntClause);
                cancelUntil(backtrackLevel);

                if (learntClause.size() == 1) {
                    uncheckedEnqueue(learntClause.get(0));
                } else {
                    Clause cr = new Clause(learntClause, true);
                    learnt.push(cr);
                    attachClause(cr);
                    clauseBumpActivity(cr);
                    uncheckedEnqueue(learntClause.get(0), cr);
                }

                variableDecayActivity();
                clauseDecayActivity();

                if (--learntSizeAdjustCnt == 0) {
                    learntSizeAdjustConflict *= learntSizeAdjustIncrement;
                    learntSizeAdjustCnt = (int) learntSizeAdjustConflict;
                    maxLearnt *= learntSizeIncrement;
                }

            } else {

                int c = numberOfVariables() - trail.size();
                if (c < cursor) {
                    cursor = c;
                    System.out.printf("\rc %.2f%% \t ", 100.0 * c / numberOfVariables());
                }

                if (nof_conflicts >= 0 && conflictCounter >= nof_conflicts ||
                        withinBudget()) {
                    cancelUntil(0);
                    return Ternary.UNDEF;
                }

                if (decisionLevel() == 0 && simplify()) {
                    return Ternary.FALSE;
                }

                if (learnt.size() - nAssigns() >= maxLearnt) {
                    reduceDB();
                }

                Literal next = Literal.UNDEF;
                while (decisionLevel() < assumptions.size()) {

                    Literal p = assumptions.get(decisionLevel());
                    if (value(p) == Ternary.TRUE) {

                        newDecisionLevel();
                    } else if (value(p) == Ternary.FALSE) {
                        analyzeFinal(p.not(), conflict);
                        return Ternary.FALSE;
                    } else {
                        next = p;
                        break;
                    }
                }

                if (next == Literal.UNDEF) {
                    next = pickBranchLit();
                    if (next == Literal.UNDEF) {
                        return Ternary.TRUE;
                    }
                }

                newDecisionLevel();
                uncheckedEnqueue(next);
            }
        }
    }

    private Ternary apply() {
        conflict.clear();
        if (!ok)
            return Ternary.FALSE;
        maxLearnt = numberOfClauses();
        learntSizeAdjustConflict = learntSizeAdjustStartConflict;
        learntSizeAdjustCnt = (int) learntSizeAdjustConflict;
        Ternary status = Ternary.UNDEF;

        int currentRestarts = 0;
        while (status == Ternary.UNDEF) {
            double rest_base = biasRestart ? activation(restartIncrement, currentRestarts) : Math.pow(restartIncrement, currentRestarts);
            status = search((int) (rest_base * restartFirst));
            if (withinBudget())
                break;
            currentRestarts++;
        }

        if (status == Ternary.TRUE) {
            model.growTo(numberOfVariables());
            for (int i = 0; i < numberOfVariables(); i++)
                model.set(i, value(Variable.valueOf(i)));
        } else if (status == Ternary.FALSE && conflict.size() == 0) {

            ok = false;
        }

        cancelUntil(0);
        return status;
    }

    private void loadCNF() throws IOException {
        BufferedReader reader = new BufferedReader(new FileReader(cnfFile));
        Parser in = new Parser(reader);
        Vector<Literal> lits = new Vector<>();
        for (; ; ) {
            String token = in.read();
            if (token == null)
                break;
            else if (token.equals("c") || token.equals("s"))
                in.skipLine();
            else if (token.equals("p")) {
                token = in.read();
                if (!token.equals("cnf"))
                    throw new IOException("PARSE ERROR! 'cnf' expected after 'p'%n");
                in.readInt();
                in.readInt();
            } else {
                lits.clear();
                for (; ; ) {
                    int parsed_lit = Integer.parseInt(token);
                    if (parsed_lit == 0) break;
                    int var = Math.abs(parsed_lit) - 1;
                    while (var >= numberOfVariables()) newVariable();
                    lits.push(Literal.valueOf(var, parsed_lit < 0));
                    token = in.read();
                }
                addClause(lits);
            }
        }
    }

    public void solve(String cnfFile) {
        this.cnfFile = cnfFile;
        try {
            loadDefault();
            loadCNF();
            long ms = System.currentTimeMillis();
            random.setSeed(ms);
            System.out.printf("c VRS : %d\n", numberOfVariables());
            System.out.printf("c CLS : %d\n", numberOfClauses());
            System.out.printf("c RHO : %.4f\n", (double) numberOfClauses() / numberOfVariables());
            if (simplify()) {
                System.out.println(String.format("c TIM : %f(s)", (double) (System.currentTimeMillis() - ms) / 1000));
                System.out.println("s UNSATISFIABLE");
                return;
            }

            Ternary ret = apply();
            System.out.println(String.format("\nc TIM : %f(s)", (double) (System.currentTimeMillis() - ms) / 1000));
            if (ret == Ternary.TRUE) {
                System.out.println("s SATISFIABLE");
            } else {
                System.out.println("s UNSATISFIABLE");
            }
            if (ret == Ternary.TRUE) {
                System.out.print("v ");
                for (int i = 0; i < numberOfVariables(); i++) {
                    if (model.get(i) != Ternary.UNDEF) {
                        System.out.printf("%s%s%d", (i == 0) ? "" : " ", (model.get(i) == Ternary.TRUE) ? "" : "-", i + 1);
                    }
                }
                System.out.print(" 0\n");
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    static class ReduceDB implements Serializable, Comparator<Clause> {
        @Override
        public int compare(Clause x, Clause y) {
            return Double.compare(x.activity(), y.activity());
        }
    }
}
