reset
set title "Precision/Recall curve for ..." 
set xlabel "Recall"
set ylabel "Precision" 
# set term ...
set terminal x11
# set ouput "filename.ext"
set xtics 0.05
set mxtics 1
set key right bottom # move legend to bottom right
set data style lp 
set point size 1.5
set size square
plot [0:1] [0:1] "prc2.tsv" using 2:3 title "prec/recall" with lines ,
     '' using 2:3:1 with labels offset 1
