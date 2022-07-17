#include <stdlib.h>
#include <stdio.h>
#include <unistd.h>
int main()
{
    if (fork() > 0)
    { // parent

        if (fork() > 0)
        {
            if (fork() > 0)
            {
                if (fork() > 0)
                {
                    system("java com/Server 0");
                }
                else
                {
                    system("java com/Server 1");
                }
            }
            else
            {
                system("java com/Server 2");
            }
        }
        else
        {
            system("java com/Client 0");
        }
    }
    else
    { // child
        if (fork() > 0)
        {
            if (fork() > 0)
            {

                system("java com/Client 1");
            }
            else
            {

                system("java com/Client 2");
            }
        }
        else
        {
            if (fork() > 0)
            {

                system("java com/Client 3");
            }
            else
            {

                system("java com/Client 4");
            }
        }
    }
}