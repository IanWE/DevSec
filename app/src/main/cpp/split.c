/*
split string by comma
*/
#include <stdio.h>
#include <string.h>
#include <stdlib.h>
#include <android/log.h>
#include "logoutput.h"

//返回一个 char *arr[], size为返回数组的长度
char **split(char sep, const char *str, int *size)
{
    size_t count = 0;
    for(int i = 0; i < strlen(str); i++)
    {
        if (str[i] == sep)
        {
            count ++;
        }
    }
    char **ret = (char**)calloc(++count, sizeof(char *));
    int lastindex = -1;
    int j = 0;
    for(int i = 0; i < strlen(str); i++)
    {
        if (str[i] == sep)
        {
            ret[j] = (char*)calloc((size_t) (i - lastindex), sizeof(char)); //分配子串长度+1的内存空间
            memcpy(ret[j], str + lastindex + 1, (size_t) (i - lastindex - 1));
            j++;
            lastindex = i;
        }
    }
    //处理最后一个子串
    int l = (int) strlen(str);
    if (lastindex < l)
    {

        ret[j] = (char*)calloc(strlen(str) - lastindex, sizeof(char));
        memcpy(ret[j], str + lastindex + 1, strlen(str) - 1 - lastindex);
        j++;
    }
    *size = j;
    return ret;
}
