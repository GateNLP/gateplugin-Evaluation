/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package gate.plugin.evaluation.tests;

import org.junit.runner.RunWith;
import org.junit.runners.Suite;

/**
 *
 * @author johann
 */
@RunWith(Suite.class)
@Suite.SuiteClasses({
  TestTagging1.class
})
public class TestAll {
  // so we can run this test from the command line 
  public static void main(String args[]) {
    org.junit.runner.JUnitCore.main(TestAll.class.getCanonicalName());
  }  
  
}
