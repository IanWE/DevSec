#coding:utf-8
from torch import nn
import torch
import torch.nn.functional as F
import torch.utils.data as Data
from torch.nn import init
from torch.autograd import Variable
import numpy as np
import pandas as pd
from sklearn.metrics import f1_score
from sklearn.model_selection import StratifiedKFold
from sklearn.metrics import classification_report
import warnings
import os
import pandas as pd
from matplotlib import cm
import pickle
import argparse
#from sparselearning.core import add_sparse_args, CosineDecay, Masking

warnings.filterwarnings('ignore')
def accuracy(y_hat, y):
    return (y_hat.argmax(axis=1) == y.astype('float32')).mean()#.asscalar()
def minmaxscaler(data,test):
    min = np.amin(data)
    max = np.amax(data)    
    return (data - min)/(max-min),(test-min)/(max-min)
def feature_normalize(data,test):
    mu = np.mean(data)
    std = np.std(data)
    return (data - mu)/std, (test-mu)/std
def to_number(x):
    if x=='BaseLine':
        return [0]
    else:
        lb = []
        for i in x:
            lb.append(int(i))
        return lb
BASE = '/data/poison/nkamg_smu/src/AISG/'

def weights_init(m):
    classname = m.__class__.__name__
    # print(classname)
    if classname.find('Conv1d') != -1:
        init.xavier_normal_(m.weight.data)
        #init.constant_(m.bias.data, 0.0)
    elif classname.find('Linear') != -1:
        init.xavier_normal_(m.weight.data)
        init.constant_(m.bias.data, 0.0)
    elif classname.find('BatchNorm1d') != -1:
        init.constant_(m.weight.data, 1)
        init.constant_(m.bias.data, 0.0)
    #elif classname.find('LSTM') != -1:
    #    init.orthogonal(m.all_weights)
    #    init.constant_(m.bias.data, 0.0)
    #elif classname.find('InstanceNorm1d') != -1:
    #    init.constant_(m.weight.data, 1)
    #    init.constant_(m.bias.data, 0.0) 

import random
def data_iter(batch_size, features, labels):
    num_examples = features.shape[0]
    indices = list(range(num_examples))
    random.seed(epoch)
    random.shuffle(indices)  # 样本的读取顺序是随机的。
    for i in range(0, num_examples/batch_size*batch_size, batch_size):
        j = indices[i: min(i + batch_size, num_examples)]
        yield (torch.FloatTensor(features[j]), torch.LongTensor(labels[j]))  # take 函数根据索引返回对应元素。

def replace_layers(model, i, indexes, layers):
    if i in indexes:
        return layers[indexes.index(i)]
    return model[i]

def adjust_learning_rate(optimizer, epoch):
    """Sets the learning rate to the initial LR decayed by 10 every 30 epochs"""
    #lr = LR * (0.3 ** (epoch // 20))
    for param_group in optimizer.param_groups:
        param_group['lr'] *= 0.8
def conv(in_planes,out_planes,kernel_size=8,stride=1):
        "3x3 convolution with padding"
        return nn.Conv1d(
                in_planes,
                out_planes,
                kernel_size,
                stride=stride,
                padding=int((kernel_size-1)/2),
                bias=False)

class BasicBlock(nn.Module):
        def __init__(self,in_planes,planes,kernel_size,stride=1,downsample=None):
                super(BasicBlock, self).__init__()
                self.conv1 = conv(in_planes,planes,kernel_size,1)
                self.bn1 = nn.BatchNorm1d(planes)
                self.relu = nn.ReLU()
                self.downsample = downsample
                self.stride = 1
                #
                self.conv2 = conv(planes,planes,kernel_size,1)
                self.bn2 = nn.BatchNorm1d(planes)
                #
                self.conv3 = conv(planes,planes,kernel_size,1)
                self.bn3 = nn.BatchNorm1d(planes)
        def forward(self,x):
                residual = x
                out = self.conv1(x)
                out = self.bn1(out)
                out = self.relu(out)
                #
                out = self.conv2(out)
                out = self.bn2(out)
                out = self.relu(out)
                #
                out = self.conv3(out)
                out = self.bn3(out)

                if self.downsample is not None:
                    residual = self.downsample(x)
                out += residual
                out = self.relu(out)
                return out

class ResNet(nn.Module):
        def __init__(self,block,kernel_size,num_classes=7,in_planes=10):#block means BasicBlock
            self.in_planes = in_planes
            super(ResNet,self).__init__()
            self.layer1 = self._make_layer(block,kernel_size[0],64)
            self.layer2 = self._make_layer(block,kernel_size[1],128)
            self.layer3 = self._make_layer(block,kernel_size[2],128)
            self.avgpool = nn.AdaptiveAvgPool1d(1)
            self.fc = nn.Linear(128,num_classes)
            #for m in self.modules():
            #    if isinstance(m, nn.Conv1d) or isinstance(m,nn.Linear):
            #        nn.init.xavier_normal_(m.weight.data)
            #        nn.init.constant_(m.bias, 0)
            #    elif isinstance(m, nn.BatchNorm1d):
            #        nn.init.constant_(m.weight.data, 1)
            #        nn.init.constant_(m.bias.data, 0)

        def _make_layer(self, block, kernel_size, planes, stride=1):
            downsample = None
            if stride != 1 or self.in_planes != planes:
                downsample = nn.Sequential(
                nn.Conv1d(self.in_planes, planes,
                kernel_size=1, stride=stride, bias=False),
                nn.BatchNorm1d(planes),
              )
            layers = []
            layers.append(block(self.in_planes,planes,kernel_size,stride,downsample))
            self.in_planes = planes
            #for i in range(1,blocks):
            #       layers.append(block(self.inplanes,planes))
            return nn.Sequential(*layers)
        def forward(self,x):
            x = self.layer1(x)
            x = self.layer2(x)
            x = self.layer3(x)
            x = self.avgpool(x)
            x = x.view(x.size(0),-1)
            x = self.fc(x)
            return x


class ResEncoder(nn.Module):
        def __init__(self,block,kernel_size,num_classes=6,in_planes=10):#block means BasicBlock
            self.in_planes = in_planes
            super(ResEncoder,self).__init__()
            self.layer1 = self._make_layer(block,kernel_size[0],64)#128)
            self.layer2 = self._make_layer(block,kernel_size[1],128)#256)
            self.layer3 = self._make_layer(block,kernel_size[2],128)#512)

            self.pool = nn.MaxPool1d(2,stride=2)
            #self.avgpool = nn.AdaptiveAvgPool1d(1)
            self.fc = nn.Linear(64,num_classes)
            self.softmax = torch.nn.Softmax(dim=1)
            #self.inm = nn.InstanceNorm1d(256)

        def _make_layer(self, block, kernel_size, planes, stride=1):
            downsample = None
            if stride != 1 or self.in_planes != planes:
                downsample = nn.Sequential(
                nn.Conv1d(self.in_planes, planes,
                kernel_size=1, stride=stride, bias=False),
                nn.BatchNorm1d(planes),
              )
            layers = []
            layers.append(block(self.in_planes,planes,kernel_size,stride,downsample))
            self.in_planes = planes
            #for i in range(1,blocks):
            #       layers.append(block(self.inplanes,planes))
            return nn.Sequential(*layers)

        def forward(self,x):
            x = self.layer1(x)
            x = self.pool(x)
            x = self.layer2(x)
            x = self.pool(x)
            x = self.layer3(x) #batch,512,60
            x = x[:,int(x.size(1)/2):,:].mul(self.softmax(x[:,:int(x.size(1)/2),:])) #batch,256,60
            x = x.sum(2)
            x = x.view(x.size(0),-1)
            #x = self.inm(x)
            x = self.fc(x)
            return x

model = ResEncoder(BasicBlock,[9,5,3],7,17);
modelx = torch.load("net_resencoder_0.pkl",map_location="cpu").module
print(model)
print("==========================================================")
print(modelx)
print("==========================================================")
model_dict = modelx.state_dict()
model.load_state_dict(model_dict)
model.eval()
example = torch.rand(1, 17, 60)
traced_script_module = torch.jit.trace(model, example)
traced_script_module.save("model.pt")
