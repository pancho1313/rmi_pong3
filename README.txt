para ejecutar super servidor:

#bin/
  rmiregistry &
  java dev.Server <<ipLocalHost>> <<num players>>

para ejecutar servidor:

#bin/
  rmiregistry &
  java dev.Server <<ipLocalHost>> <<ipSuperServer>>
  


para ejecutar cliente

#bin/
  rmiregistry &
  java dev.Client <<ipLocalHost>> <<ipSuperServer>>
  


para compilar

#/
  javac -d bin src/dev/*.java
