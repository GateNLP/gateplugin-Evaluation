
package gate.plugin.evaluation.api;

import gate.util.NameBearer;
import java.util.ArrayList;
import java.util.List;

/**
 * Simple class that represents a contingency table.
 * A contingency table is a NxM matrix which also knows about the row and column labels
 * and which knows its name.
 * 
 * @author Johann Petrak
 */
public class ContingencyTable implements NameBearer {

  protected String name = "";
  @Override
  public void setName(String string) {
    name = string;
  }

  @Override
  public String getName() {
    return name;
  }
  
  
  protected List<String> rowLabels;
  protected List<String> columnLabels;
  protected int rows;
  protected int cols;
  protected int[] values;
  
  private ContingencyTable() {}
  
  public ContingencyTable(int rows, int cols) {
    this.rows = rows;
    this.cols = cols;
    rowLabels = new ArrayList<String>(rows);
    columnLabels = new ArrayList<String>(cols);
    values = new int[rows*cols];    
  }
  
  public void setRowLabels(List<String> rls) {
    if(rls.size() != rowLabels.size()) {
      throw new RuntimeException("List of row labels must have size "+rowLabels.size());
    }
    for(int i = 0; i < rowLabels.size(); i++) {
      rowLabels.set(i, rls.get(i));
    }
  }
  public void setColumnLabels(List<String> cls) {
    if(cls.size() != columnLabels.size()) {
      throw new RuntimeException("List of column labels must have size "+columnLabels.size());
    }
    for(int i = 0; i < columnLabels.size(); i++) {
      columnLabels.set(i, cls.get(i));
    }
  }
  
  public void setRowLabel(int row, String label) {
    rowLabels.set(row, label);
  }
  public void setColumnLabel(int row, String label) {
    rowLabels.set(row, label);
  }
  public List<String> getRowLabels() { return rowLabels; }
  public List<String> getColumnLabels() { return columnLabels; }
  
  public String getRowLabel(int i) { return rowLabels.get(i); }
  public String getColumnLabel(int i) { return columnLabels.get(i); }
  
  public void set(int row, int col, int value) {
    values[rows*row+col] = value;
  }
  
  public void incrementBy(int row, int col, int value) {
    values[rows*row+col] += value;
  }
  
  public int get(int row, int col) {
    return values[rows*row+col];
  }
  
  public int nRows() { return rows; }
  public int nColumns() { return cols; }
  
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("ContingencyTable: ");
    sb.append(getName()); sb.append("\n");
    for(int r = 0; r < rows; r++) {
      for(int c = 0; c < cols; c++) {
        sb.append("row=");
        sb.append(getRowLabel(r));
        sb.append(", col=");
        sb.append(getColumnLabel(c));
        sb.append(": ");
        sb.append(get(r, c));
        sb.append("\n");
      }
    }
    return sb.toString();
  }

  // TODO: implement a proper content-based hashCode and equals method.

}
