package model;

import gui.Chart;
import gui.SimulationPanel;

import java.util.ArrayList;

import utils.Utils;
import bankersalgo.BankersAlgorithm;
import bankersalgo.SafeState;

public class Simulation extends Thread {
	private ArrayList<Process> process;
	private ArrayList<Integer> available;
	private final Object GUI_MONITOR=new Object();
	
	private int resourceNum;
	private int simCount;
	private int delay=1000;
	private boolean running=true, pauseThreadFlag;
	private int maxIteration=0;
	
	private SimulationPanel simPanel;
	private SchedulingAlgo[] cpu;
	private BankersAlgorithm[] banker;
	private DiskAlgo[] disk;
	private Chart[] chart;
	
	public Simulation(Input input,SimulationPanel simPanel, boolean state) {
		this.simPanel=simPanel;
		pauseThreadFlag=state;
		simCount=input.getSize();
		cpu=new SchedulingAlgo[simCount];
		banker=new BankersAlgorithm[simCount];
		disk=new DiskAlgo[simCount];
		chart=new Chart[simCount];

		resourceNum=input.getResourceNum();
		available=input.getAvailable();
		process=input.getProcess();
		for (int i=0; i<simCount; i++) {
			chart[i]=new Chart(i+1,input.getHeadCylinder(),input.getMaxCylinder(),input.getTime());
			banker[i]=input.getBankers(i,chart[i]);
			cpu[i]=input.getCPUSched(i,chart[i]);
			disk[i]=input.getDiskAlgo(i,chart[i]);
			simPanel.addChart(chart[i]);
		}
		init();
	}
	
	private void init() {
		SafeState deadlock=new SafeState(available);
		
		while (deadlock.hasDeadlock(process, resourceNum)) {
			deadlock.pushSafeState(available);
			available=deadlock.getAvailable();
		}
		maxIteration = deadlock.getMaxIteration();
		start();
	}

	
	public void pause() {
//		if (pauseThreadFlag) {
//			delay=0;
//			interrupt();
//			pauseThreadFlag=false;
//		}
//		else {
//			pauseThreadFlag=true;
//		}
	}
	
	public void end() {
		running=false;
		interrupt();
	}

	public void run() {
		ArrayList<Process> tempProc;
		int t=0;
		int done=0;

		while (running) {
			if (!isInterrupted()) {
				for (int i=0; i<simCount; i++) {
					tempProc = banker[i].getProcess(t, maxIteration);
					
					if (tempProc.size()>0) {
						cpu[i].addNewProc(tempProc);
						disk[i].addList(tempProc);
					}
					
					cpu[i].execute(t);
					disk[i].execute(t+1, cpu[i].getCurrProc());
					
					if (cpu[i].hasReleased()) {
						banker[i].releaseRes(cpu[i].getReleased());
					}
					if  (cpu[i].isDone()) {
						cpu[i].set();
						done++;
					}
					
					if (done==simCount) {
						running=false;
						chart[i].showStat(Utils.mergeList(cpu[i].getProcessSummary(i),
								disk[i].getProcessSummary()),"<html>"+cpu[i].getSummary()+
								disk[i].getTotal()+"</html>"); 
						break;
					}
					banker[i].resetAllocated();
				}
				t++;
				simPanel.changeName();
			}
			
			try {
				Thread.sleep(delay);
//				while (!pauseThreadFlag) {
//					interrupt();
//				}
			} catch(Exception e) {}
		}
	}
}