/*
 *  Copyright (c) 1995-2015, The University of Sheffield. See the file
 *  COPYRIGHT.txt in the software or at http://gate.ac.uk/gate/COPYRIGHT.txt
 *
 *  This file is part of GATE (see http://gate.ac.uk/), and is free
 *  software, licenced under the GNU Library General Public License,
 *  Version 2, June 1991 (in the distribution as file licence.html,
 *  and also available at http://gate.ac.uk/gate/licence.html).
 *
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
  TestTagging1.class,
  TestTagging2.class,
  //TestTagging3.class,
  //TestTagging4.class
})
public class TestAll {
  // so we can run this test from the command line 
  public static void main(String args[]) {
    org.junit.runner.JUnitCore.main(TestAll.class.getCanonicalName());
  }  
  
}
