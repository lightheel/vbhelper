package com.github.nacabaro.vbhelper.battle

import android.content.Context
import java.io.File
import java.io.FileOutputStream
import java.io.IOException

class SpriteFileManager(private val context: Context) {
    
    fun copySpriteFilesToInternalStorage() {
        try {
            // Create the base directory
            val baseDir = File(context.filesDir, "Battle_Sprites_Reference/extracted_assets")
            if (!baseDir.exists()) {
                baseDir.mkdirs()
            }
            
            // Copy files from assets to internal storage
            copyAssetDirectory("Battle_Sprites_Reference/extracted_assets", baseDir)
            
            println("Sprite files copied successfully to: ${baseDir.absolutePath}")
            
        } catch (e: Exception) {
            println("Error copying sprite files: ${e.message}")
            e.printStackTrace()
        }
    }
    
    private fun copyAssetDirectory(assetPath: String, targetDir: File) {
        try {
            val assetManager = context.assets
            val files = assetManager.list(assetPath) ?: return
            
            for (file in files) {
                val assetFilePath = if (assetPath.isEmpty()) file else "$assetPath/$file"
                val targetFile = File(targetDir, file)
                
                // Create subdirectories if needed
                if (targetFile.parentFile != null && !targetFile.parentFile!!.exists()) {
                    targetFile.parentFile!!.mkdirs()
                }
                
                // Check if it's a directory
                val subFiles = assetManager.list(assetFilePath)
                if (subFiles != null && subFiles.isNotEmpty()) {
                    // It's a directory, create it and copy contents
                    if (!targetFile.exists()) {
                        targetFile.mkdirs()
                    }
                    copyAssetDirectory(assetFilePath, targetFile)
                } else {
                    // It's a file, copy it
                    copyAssetFile(assetFilePath, targetFile)
                }
            }
        } catch (e: Exception) {
            println("Error copying asset directory $assetPath: ${e.message}")
        }
    }
    
    private fun copyAssetFile(assetPath: String, targetFile: File) {
        try {
            val inputStream = context.assets.open(assetPath)
            val outputStream = FileOutputStream(targetFile)
            
            inputStream.copyTo(outputStream)
            inputStream.close()
            outputStream.close()
            
            println("Copied: $assetPath -> ${targetFile.absolutePath}")
        } catch (e: IOException) {
            println("Error copying asset file $assetPath: ${e.message}")
        }
    }
    
    fun checkSpriteFilesExist(): Boolean {
        val baseDir = File(context.filesDir, "Battle_Sprites_Reference/extracted_assets")
        return baseDir.exists() && baseDir.listFiles()?.isNotEmpty() == true
    }
} 