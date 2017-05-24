import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

import org.ggp.base.util.statemachine.Role;
import org.ggp.base.util.statemachine.StateMachine;

public class TreeThreadedMonteCarloTreeSearch extends MonteCarloTreeSearch {

	public TreeThreadedMonteCarloTreeSearch(StateMachine machine, Role role) {
		super(machine, role);
	}

	public static final int NUM_THREADS = 4;

	class TreeThread extends Thread {
		MonteCarloTreeSearch tree;
		public TreeThread(MonteCarloTreeSearch tree) {
			this.tree = tree;
		}

		@Override
		public void run() {
			try {
                while (!Thread.interrupted()) {
                    tree.search();
                }
			} catch(Exception exc) {
				System.out.println("[THREAD EXCEPTION] Malformed game!");
			}
		}
	}

	private List<MonteCarloTreeSearch> searchTrees(Duration searchTime) {
        List<MonteCarloTreeSearch> trees = new ArrayList<MonteCarloTreeSearch>();
        List<TreeThread> threads = new ArrayList<TreeThread>();
		try {
			// 1. Construct trees and start threads
            for (int i = 0; i < NUM_THREADS; i++) {
                MonteCarloTreeSearch tree = new MonteCarloTreeSearch(stateMachine, role);//, root.state);
                TreeThread thread = new TreeThread(tree);
                trees.add(tree);
                thread.start();
            }
            // 2. Wait for searchTime
            Thread.sleep(searchTime.toMillis());
            // 3. Interrupt & join threads
            for (TreeThread thread : threads)
                thread.interrupt();
            for (TreeThread thread : threads)
                thread.join();
		} catch (InterruptedException e) {
				e.printStackTrace();
		}
		return trees;
	}

	@Override
	public void search(Duration searchTime) {
		System.out.println("ThreadedMonteCarloTreeSearch: searching trees");
		List<MonteCarloTreeSearch> trees = searchTrees(searchTime);
		System.out.println("ThreadedMonteCarloTreeSearch: combining trees");
		for (MonteCarloTreeSearch tree : trees) {
			System.out.println("children before combine: " + root.children.size());
			System.out.println("children before combine: " + tree.root.children.size());
			//root.combineTree(tree.root);
			System.out.println("children after: " + root.children.size());
		}
	}
}
