package kodkod.engine.satlab;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;

/**
 * Runs an external version of minisat (whichever "minisat" is found in the PATH)
 * 
 * @author aleks
 */
public class MiniSatExternal implements SATSolver {

    private File cnfFile; 
    private OutputStream cnfOut; 
    
    private int vars = 0;
    private int clauses = 0;
    private boolean freed = false; 
    
    private boolean sat = false;
    private boolean[] model; 
    
    public MiniSatExternal() {
        init();
    }

    private void init() {
        try {
            cnfFile = File.createTempFile("kk_minisat", ".cnf");
            cnfOut = new BufferedOutputStream(new FileOutputStream(cnfFile));
        } catch (IOException e) {
            throw new RuntimeException("Could not create cnf file: " + cnfFile.getAbsolutePath(), e);
        }
    }

    @Override
    public boolean addClause(int[] lits) {
        try {
            clauses++;
            StringBuilder sb = new StringBuilder();
            for (int l : lits) sb.append(l + " ");
            sb.append("0\n");
            cnfOut.write(sb.toString().getBytes());
            return false;
        } catch (IOException e) {
            throw new RuntimeException("Could not write to cnf file " + cnfFile.getAbsolutePath(), e);
        }
    }

    @Override
    public void addVariables(int numVars) {
        assert numVars >= 0; 
        vars += numVars;
    }

    @Override
    public void free() {
        try {
            if (!freed) {
                cnfOut.flush();
                cnfOut.close();
                cnfFile.deleteOnExit();
                freed = true;
            }
        } catch (IOException e) {
            throw new RuntimeException("Could not close cnf output stream");
        }
    }

    @Override
    public int numberOfClauses() {
        return clauses;
    }

    @Override
    public int numberOfVariables() {
        return vars;
    }

    @Override
    public boolean solve() throws SATAbortedException {
        finalizeCnf(); 
        File modelFile = runMinisat();
        readModel(modelFile);
        if (sat) 
            assert checkModel();
        return sat;
    }

    private boolean checkModel() {
        BufferedReader br = null; 
        try {
            br = new BufferedReader(new InputStreamReader(new FileInputStream(cnfFile)));
            String line = br.readLine(); //header
            line = br.readLine();
            while (line != null) {
                String[] clause = line.trim().split("\\ ");
                int i; 
                for (i = 0; i < clause.length - 1; i++) {
                    int lit = Integer.parseInt(clause[i]);
                    if ((lit > 0 ? valueOf(lit) : !valueOf(Math.abs(lit))))
                        break;
                }
                if (i == clause.length - 1)
                    return false;
                line = br.readLine();
            }
            return true;
        } catch (IOException e) {
            throw new RuntimeException(e);
        } finally {
            try {
                if (br != null) br.close();
            } catch (IOException e) {
            }
        }
    }

    @Override
    public boolean valueOf(int variable) {
        if (!sat)
            throw new RuntimeException("UNSAT");
        return model[variable-1];
    }
    
    private void finalizeCnf() {
        try {
            free();
            BufferedInputStream in = new BufferedInputStream(new FileInputStream(cnfFile));
            File tmp = new File(cnfFile + ".tmp"); 
            BufferedOutputStream out = new BufferedOutputStream(new FileOutputStream(tmp)); 
            byte[] header = String.format("p cnf %s %s\n", vars, clauses).getBytes();
            out.write(header);
            int ch;
            while ((ch = in.read()) != -1) {
                out.write(ch);
            }
            in.close();
            out.close();
            tmp.renameTo(cnfFile);
        } catch (IOException e) {
            throw new RuntimeException("could not preprend header to cnf file: " + cnfFile.getAbsolutePath(), e);
        }
    }
    
    private File runMinisat() {
        try {
            File modelFile = File.createTempFile("minisat_model", ".txt");
            modelFile.deleteOnExit();
            String cmd = String.format("minisat -verb=0 %s %s", cnfFile.getAbsolutePath(), modelFile.getAbsolutePath());
            Process p = Runtime.getRuntime().exec(cmd);
            int retVal = p.waitFor();
            if (retVal == 0)
                throw new RuntimeException("couldn't run shell command: " + cmd);
            return modelFile;
        } catch (Exception e) {
            throw new RuntimeException("error during solving", e);
        }
    }
    
    private void readModel(File modelFile) {
        try {
            BufferedInputStream in = new BufferedInputStream(new FileInputStream(modelFile));
            String line = readLine(in).trim();
            if (!"SAT".equals(line)) {
                sat = false;
                model = null;
                return;
            }
            sat = true; 
            model = new boolean[vars];
            int chCode;
            int cnt = 0;
            int curr = 0; 
            boolean sign = true;
            while ((chCode = in.read()) != -1) {
                char ch = (char)chCode;
                switch (ch) {
                case '-': 
                    sign = false; 
                    break;
                case ' ': case '\n':
                    if (curr == 0)
                        break;
                    assert curr == cnt + 1; 
                    model[cnt] = sign;
                    cnt++; 
                    curr = 0; 
                    sign = true;
                    break;
                default:    
                    int x = ch - '0';
                    assert x >= 0 && x <= 9; 
                    curr = 10*curr + x;
                    break;
                }
            }
            in.close();
        } catch (IOException e) {
            throw new RuntimeException("couldn't read model file: " + model);
        }
    }

    private String readLine(InputStream in) throws IOException {
        StringBuilder sb = new StringBuilder();
        while (true) {
            int ch = in.read();
            if (ch == -1) break;
            if (ch == '\n') break;
            sb.append((char)ch);
        }
        return sb.toString();
    }

}
