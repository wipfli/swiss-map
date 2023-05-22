#include "raqm.h"

int
main (int argc, char *argv[])
{
    const char *fontfile;
    const char *text;
    const char *direction;
    const char *language;
    int ret = 1;

    FT_Library library = NULL;
    FT_Face face = NULL;

    if (argc < 5)
    {
        printf ("Usage: %s FONT_FILE TEXT DIRECTION LANG\n", argv[0]);
        return 1;
    }

    fontfile =  argv[1];
    text = argv[2];
    direction = argv[3];
    language = argv[4];

    if (FT_Init_FreeType (&library) == 0)
    {
      if (FT_New_Face (library, fontfile, 0, &face) == 0)
      {
        if (FT_Set_Char_Size (face, face->units_per_EM, 0, 0, 0) == 0)
        {
          raqm_t *rq = raqm_create ();
          if (rq != NULL)
          {
            raqm_direction_t dir = RAQM_DIRECTION_DEFAULT;

            if (strcmp (direction, "r") == 0)
              dir = RAQM_DIRECTION_RTL;
            else if (strcmp (direction, "l") == 0)
              dir = RAQM_DIRECTION_LTR;

            if (raqm_set_text_utf8 (rq, text, strlen (text)) &&
                raqm_set_freetype_face (rq, face) &&
                raqm_set_par_direction (rq, dir) &&
                raqm_set_language (rq, language, 0, strlen (text)) &&
                raqm_layout (rq))
            {
              size_t count, i;
              raqm_glyph_t *glyphs = raqm_get_glyphs (rq, &count);

              ret = !(glyphs != NULL || count == 0);

              for (i = 0; i < count; i++)
              {
                  printf ("%d %d %d %d %d %d\n",
                          glyphs[i].index,
                          glyphs[i].x_offset,
                          glyphs[i].y_offset,
                          glyphs[i].x_advance,
                          glyphs[i].y_advance,
                          glyphs[i].cluster);
              }
            }

            raqm_destroy (rq);
          }
        }

        FT_Done_Face (face);
      }

      FT_Done_FreeType (library);
    }

    return ret;
}