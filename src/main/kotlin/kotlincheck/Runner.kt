package kotlincheck

import java.io.File
import java.net.URLClassLoader

class Runner {
    fun loadClasses(classPaths: String) = loadClasses(classPaths.split(";"))
    fun loadClasses(classPaths: List<String>) {
        classPaths.forEach {
            val url = File(System.getProperty("user.dir")).toURI().toURL()
            println(url)
            val urlClassLoader = URLClassLoader(arrayOf(url))
            val clazz = urlClassLoader.loadClass(it)
            val instance = clazz.getConstructor().newInstance()
            println("Running methods of $it:")
            clazz.methods.map { m ->
                if (m.declaringClass == clazz) {

                    println("Running " + m.name + ":")
                    m.invoke(instance)
                }
            }
        }
    }
}

fun printOnNumberFormatException(input: String) {
    println("Failed to parse $input param. Using default value")
}

fun main(args: Array<String>) {
    if (args.isEmpty())
        return
    var seed: Long = 1
    var minSuccesfulTest = 100
    var minSize = 20
    var maxSize = 100
    var workers = 5
    for (i in 1..(args.size - 1)) {
        val split = args[i].split("=")
        if (split.size != 2)
            continue
        when (split[0]) {
            "seed" -> try {
                seed = split[1].toLong()
            } catch (e: NumberFormatException) {
                printOnNumberFormatException(split[0])
            }
            "minSuccesfulTest" -> try {
                minSuccesfulTest = split[1].toInt()
            } catch (e: NumberFormatException) {
                printOnNumberFormatException(split[0])
            }
            "minSize" -> try {
                minSize = split[1].toInt()
            } catch (e: NumberFormatException) {
                printOnNumberFormatException(split[0])
            }
            "maxSize" -> try {
                maxSize = split[1].toInt()
            } catch (e: NumberFormatException) {
                printOnNumberFormatException(split[0])
            }
            "workers" -> try {
                workers = split[1].toInt()
            } catch (e: NumberFormatException) {
                printOnNumberFormatException(split[0])
            }
        }
    }
    Test.params = Test.Parameters(seed, minSuccesfulTest, minSize, maxSize, workers)
    Runner().loadClasses(args[0])
}