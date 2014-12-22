package ca.mcgill.mcb.pcingola.bigDataScript.test;

import org.junit.Test;

/**
 * Quick test cases when creating a new feature...
 *
 * @author pcingola
 *
 */
public class TestCasesZzz extends TestCasesBase {

	@Test
	public void test121_split_empty_string() {
		runAndCheckpoint("test/z.bds", "test/z.chp", "ok", "true");
	}

	@Test
	public void test122_checkpoint_recursive() {
		runAndCheckpoint("test/zz.bds", "test/zz.chp", "fn", "120");
	}

}
