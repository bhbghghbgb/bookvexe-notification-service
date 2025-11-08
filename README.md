### repo list

https://github.com/bhbghghbgb/bookvexe-notification-service

https://github.com/ndthongsgu/bookvexe-api-gateway
https://github.com/ndthongsgu/bookvexe-chat-service
https://github.com/ndthongsgu/bookvexe-config-service
https://github.com/ndthongsgu/bookvexe-discovery-server
https://github.com/bhbghghbgb/bookvexe-be-j2e
https://github.com/GDevNeON/bookvexe-payment-service

https://github.com/HoangHoan04/bms-admin
https://github.com/HoangHoan04/bms-user

### split repo log

```
F:\>git clone -b feat-notification+mail https://github.com/bhbghghbgb/bookvexe-be-j2e.git bookvexe-notification-service
Cloning into 'bookvexe-notification-service'...
remote: Enumerating objects: 5581, done.
remote: Counting objects: 100% (1199/1199), done.
remote: Compressing objects: 100% (405/405), done.
remote: Total 5581 (delta 734), reused 1067 (delta 664), pack-reused 4382 (from 1)
Receiving objects: 100% (5581/5581), 4.88 MiB | 7.93 MiB/s, done.
Resolving deltas: 100% (3621/3621), done.

F:\>cd bookvexe-notification-service

F:\bookvexe-notification-service>git filter-repo --path mail-service/ --path notification-service/
NOTICE: Removing 'origin' remote; see 'Why is my origin removed?'
        in the manual if you want to push back there.
        (was https://github.com/bhbghghbgb/bookvexe-be-j2e.git)
Parsed 181 commits
New history written in 0.84 seconds; now repacking/cleaning...
Repacking your repo and cleaning out old unneeded objects
HEAD is now at 449ac0b [wip] (7) (be -> kafka -> mail OK) kafka type header mapping, mail service need db for JobLauncher, add send notification email override method
Enumerating objects: 357, done.
Counting objects: 100% (357/357), done.
Delta compression using up to 4 threads
Compressing objects: 100% (149/149), done.
Writing objects: 100% (357/357), done.
Total 357 (delta 158), reused 332 (delta 157), pack-reused 0 (from 0)
Completely finished after 1.38 seconds.

F:\bookvexe-notification-service>git remote add origin https://github.com/bhbghghbgb/bookvexe-notification-service.git

F:\bookvexe-notification-service>git push origin HEAD:master
Enumerating objects: 357, done.
Counting objects: 100% (357/357), done.
Delta compression using up to 4 threads
Compressing objects: 100% (148/148), done.
Writing objects: 100% (357/357), 49.59 KiB | 9.92 MiB/s, done.
Total 357 (delta 158), reused 357 (delta 158), pack-reused 0 (from 0)
remote: Resolving deltas: 100% (158/158), done.
To https://github.com/bhbghghbgb/bookvexe-notification-service.git
 * [new branch]      HEAD -> master
```