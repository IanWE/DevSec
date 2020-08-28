#-*- coding:utf-8 -*-
import lief

oat = lief.DEX.parse("jar/classes.dex")
cls_ofs = dict()
print("It has "+str(len(oat.classes))+" classes")
for i in range(len(oat.classes)):
   print("Class "+str(i)+": "+str(oat.classes[i]))
   print("=========================================")
   for mindex,x in enumerate(oat.classes[i].methods):
       #if x.code_offset!=0:
       print("--- Method "+str(mindex)+" "+str(x)+hex(x.code_offset))
   print("=========================================")
