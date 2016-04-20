# checkout clean code #

```
svn checkout https://cellbots.googlecode.com/svn/trunk/ cellbots
cd cellbots
```

# create branch #

(if you choose to reuse existing code-review-branch, skip this)

```
svn cp https://cellbots.googlecode.com/svn/trunk \
  https://cellbots.googlecode.com/svn/branches/your-branch \
  -m "Branching from trunk to your-branch"
```

# switch to branch #

```
svn switch https://cellbots.googlecode.com/svn/branches/your-branch
svn update
```

# merge with trunk #

(harmless if you're creating a new branch, must do otherwise)
```
svn merge https://cellbots.googlecode.com/svn/trunk/

svn commit -m "merging branch your-branch with main trunk/"
```

# code #

make your changes, build, test, etc.

# commit to branch #

verify that you're on the right branch
```
svn info | grep URL
```
should show https://cellbots.googlecode.com/svn/branches/your-branch

commit
```
svn commit -m "your commit message"
```

# request code review #

go to http://code.google.com/p/cellbots/source/list and look at the change you submitted. get it reviewed.

# merge back to main trunk #

on your-branch
go do "**merge with trunk**" on branch (just in case someone/something else submitted)

then switch to main trunk
```
svn switch https://cellbots.googlecode.com/svn/trunk/
svn merge --reintegrate ^/branches/your-branch
```

# commit to main trunk #

```
svn commit -m "your commit message; originally reviewed as [id] in your-branch"
```

# enjoy #

smile :)