package pt.vendaclara.app.dados

import android.content.Context
import androidx.room.ColumnInfo
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.Insert
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.Update
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import kotlinx.coroutines.flow.Flow

enum class EstadoArtigo(val rotulo: String) {
    POR_PUBLICAR("Por publicar"),
    ANUNCIADO("Anunciado"),
    VENDIDO("Vendido")
}

@Entity(tableName = "artigos")
data class ArtigoEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val titulo: String = "",
    @ColumnInfo(defaultValue = "''") val descricao: String = "",
    @ColumnInfo(defaultValue = "'{}'") val detalhes: String = "{}",
    @ColumnInfo(defaultValue = "'[]'") val perguntas: String = "[]",
    val estado: String = EstadoArtigo.POR_PUBLICAR.name,
    val criadoEm: Long,
    val atualizadoEm: Long
)

@Entity(
    tableName = "fotos",
    foreignKeys = [
        ForeignKey(
            entity = ArtigoEntity::class,
            parentColumns = ["id"],
            childColumns = ["artigoId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("artigoId")]
)
data class FotoEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val artigoId: Long,
    val caminho: String,
    val ordem: Int
)

@Dao
interface ArtigoDao {
    @Insert
    suspend fun inserirArtigo(artigo: ArtigoEntity): Long

    @Update
    suspend fun atualizarArtigo(artigo: ArtigoEntity)

    @Query("SELECT * FROM artigos WHERE id = :id")
    suspend fun obterArtigo(id: Long): ArtigoEntity?

    @Query("DELETE FROM artigos WHERE id = :id")
    suspend fun apagarArtigo(id: Long)

    @Query("SELECT * FROM artigos ORDER BY atualizadoEm DESC")
    fun observarArtigos(): Flow<List<ArtigoEntity>>

    @Insert
    suspend fun inserirFoto(foto: FotoEntity): Long

    @Query("DELETE FROM fotos WHERE id = :id")
    suspend fun apagarFoto(id: Long)

    @Query("SELECT * FROM fotos WHERE artigoId = :artigoId ORDER BY ordem, id")
    fun observarFotos(artigoId: Long): Flow<List<FotoEntity>>

    @Query("SELECT * FROM fotos WHERE artigoId = :artigoId ORDER BY ordem, id")
    suspend fun obterFotos(artigoId: Long): List<FotoEntity>

    @Query("SELECT * FROM fotos ORDER BY ordem, id")
    fun observarTodasFotos(): Flow<List<FotoEntity>>
}

@Database(entities = [ArtigoEntity::class, FotoEntity::class], version = 2, exportSchema = false)
abstract class BaseDados : RoomDatabase() {
    abstract fun artigoDao(): ArtigoDao

    companion object {
        @Volatile
        private var instancia: BaseDados? = null

        fun obter(context: Context): BaseDados =
            instancia ?: synchronized(this) {
                instancia ?: Room.databaseBuilder(
                    context.applicationContext,
                    BaseDados::class.java,
                    "venda_clara.db"
                ).addMigrations(MIGRACAO_1_2).build().also { instancia = it }
            }
    }
}

val MIGRACAO_1_2 = object : Migration(1, 2) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE artigos ADD COLUMN descricao TEXT NOT NULL DEFAULT ''")
        db.execSQL("ALTER TABLE artigos ADD COLUMN detalhes TEXT NOT NULL DEFAULT '{}'")
        db.execSQL("ALTER TABLE artigos ADD COLUMN perguntas TEXT NOT NULL DEFAULT '[]'")
    }
}
