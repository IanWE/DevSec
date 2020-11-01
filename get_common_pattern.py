import os

l=os.popen("cat pattern_analysis3.txt").read().split("=============================================")
l0 = l[0]
l1 = l[1]

l0 = l0.split("\n")
l0 = filter(lambda x:"***" in x,l0)
l0 = list(l0)
ll0 = list(map(lambda x:x.split(" Pattern ")[1],l0))
ll0 = list(filter(lambda x:x in l1,ll0))
ll0 = list(map(lambda x:x.split(" ***")[0],ll0))

s = ""
for i in ll0:
    s+='"'+i+'"'+","

print(s)
print(str(len(ll0)))
