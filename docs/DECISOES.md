# Decisões e desvios da especificação

Formato: data · decisão · motivo.

## Decisões da especificação (registro exigido por ela)

- **30/09/2026 · Conflito de sync: alteração local pendente vence.** O download nunca sobrescreve item com entrada no outbox, e o `PATCH` envia só os campos alterados (`changedFields`). Motivo: edições em campos diferentes no celular e no Google sobrevivem (spec 7.2).
- **30/09/2026 · Limitação conhecida de alarmes: Direct Boot fica fora do MVP.** O `BOOT_COMPLETED` só chega depois do primeiro desbloqueio após reiniciar; um alarme que cairia antes desse desbloqueio não toca. Solução futura: `LOCKED_BOOT_COMPLETED`, componentes `directBootAware` e cópia dos próximos alarmes em `createDeviceProtectedStorageContext()` (spec 7.4).

## Ambiente e versões

- **30/09/2026 · Projeto esqueleto ainda não existe (M0 bloqueado no passo 3.1).** O repositório foi clonado só com a documentação; não há `gradlew`, Android SDK nem Android Studio nesta máquina (há JDK 25 do sistema e IntelliJ IDEA). A spec manda partir do esqueleto gerado pelo Android Studio. Enquanto isso, só a documentação do M0 é preparada.
- **12/10/2026 · Esqueleto criado pelo Gustavo** no Android Studio Rabbit 1 (2026.2.1.8), Empty Activity, `com.sincronia.app`, minSdk 26. O template trouxe `compileSdk`/`targetSdk` 37.
- **12/10/2026 · Versões do M0** (`gradle/libs.versions.toml`; as do template foram mantidas, as bibliotecas desatualizadas subiram para a última estável no Google Maven / Maven Central nesta data):
  Gradle 9.6.0 · AGP 9.4.1 (Kotlin embutido) · Kotlin/plugin Compose 2.2.10 · KSP 2.3.12 · Compose BOM 2026.09.00 · Navigation Compose 2.10.2 · Hilt 2.60.1 · androidx.hilt (hilt-work) 1.4.0 · WorkManager 2.12.0 · core-ktx 1.19.1 · lifecycle 2.11.0 · activity-compose 1.13.0. Room, DataStore, Retrofit etc. entram nos marcos em que forem usados.
- **12/10/2026 · Build com o JDK do Android Studio** (`android-studio/jbr`). Motivo: o Java do sistema é o 25; usar o mesmo JDK do Studio evita um segundo daemon do Gradle. No terminal: `JAVA_HOME=~/Documents/Workspace/android-studio/jbr ./gradlew …`.
- **12/10/2026 · `compileOptions` em Java 17** (o template trouxe 11). Motivo: a spec pede JDK 17.

## Interface

- **12/10/2026 · Tema escuro e tons não fixados pela seção 9 gerados com `material-color-utilities` 0.3.0** (`SchemeTonalSpot`, semente `#1565C0`), o mesmo algoritmo do Material Theme Builder. **Desvio:** o `tertiary` gerado é rosado (`#DCBCE1` no escuro, container `#F9D8FD` no claro); foi trocado pela paleta tonal de `#42A5F5` (tons 80/20/30/90 no escuro, 90/10 no container claro). Motivo: a paleta da marca é só azul.
- **12/10/2026 · Ícones das abas como vetores em `res/drawable`** (Material Icons, Apache 2.0) em vez da biblioteca `material-icons`. Motivo: a biblioteca não está mais no BOM do Compose e seriam 4 ícones.
- **12/10/2026 · Rotas das abas em texto** (`today`, `agenda`, `tasks`, `settings`) em vez de rotas tipadas com kotlinx.serialization. Motivo: solução mais simples para o M0; dá para migrar quando houver telas com argumentos (editor, M4).
- **12/10/2026 · `AppLogger` grava em `files/logs/app.log` com rotação em 512 KB** (um backup `app.log.1`, no máximo ~1 MB no aparelho), numa thread própria. Motivo: o "Exportar diagnóstico" (F10) precisa de um arquivo, sem ocupar espaço sem limite.
