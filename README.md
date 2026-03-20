# memory-allocation-simulator
Visual Java Swing simulator for First Fit, Best Fit, and Worst Fit memory allocation
Run
bashjavac MemoryAllocationGUI.java
java MemoryAllocationGUI
How to use

Enter number of blocks and processes
Fill in each block size (KB)
Fill in each process size (KB)
Click First Fit, Best Fit, or Worst Fit
Click Summary to compare all three

What you see

Colour-coded memory map — each process gets its own colour
Orange = internal fragmentation (wasted space inside a block)
Dark = free / unallocated block
Stats cards — internal frag, external frag, allocated, total memory
Summary panel — compares all strategies and highlights the best one

Requirements
Java 8 or higher. No external libraries.
