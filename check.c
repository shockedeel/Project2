#include <stdlib.h>
#include <stdio.h>
int files_are_correct(char *[]);
int main()
{

    printf("Checking to see if all files match...\n");
    char *file_names_a[3] = {"s1/a.txt", "s2/a.txt", "s3/a.txt"};
    int a = files_are_correct(file_names_a);
    char *file_names_b[3] = {"s1/b.txt", "s2/b.txt", "s3/b.txt"};
    int b = files_are_correct(file_names_b);
    char *file_names_c[3] = {"s1/c.txt", "s2/c.txt", "s3/c.txt"};
    int c = files_are_correct(file_names_c);
    if (c > 0 && b > 0 && a > 0)
    {
        printf("\n\nAll files checked, all files matching...\n\n");
    }
    if (a == 0)
    {
        printf("A wrong\n");
    }
    if (b == 0)
    {
        printf("B wrong\n");
    }
    if (c == 0)
    {
        printf("C wrong\n");
    }
    return 0;
}

int files_are_correct(char *file_names[])
{
    char ch1;
    char ch2;
    char ch3;
    FILE *fp1;
    FILE *fp2;
    FILE *fp3;
    fp1 = fopen(file_names[0], "r");
    fp2 = fopen(file_names[1], "r");
    fp3 = fopen(file_names[2], "r");
    if (fp1 == NULL || fp2 == NULL || fp3 == NULL)
    {
        perror("error opening one of the files...");
        return -1;
    }
    ch1 = 'a';
    int flag = 0;
    while (ch1 != EOF)
    {
        ch1 = fgetc(fp1);
        ch2 = fgetc(fp2);
        ch3 = fgetc(fp3);
        if (ch1 != ch2 || ch1 != ch3 || ch2 != ch3)
        {
            flag = 1;
            break;
        }
    }
    if (flag)
    {
        printf("There was a descripancy in the files...");
        return 0;
    }
    return 1;
}