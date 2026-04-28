import {
  Card,
  CardActions,
  CardContent,
  Chip,
  IconButton,
  Link as MuiLink,
  Stack,
  Typography,
} from '@mui/material';
import BookmarkIcon from '@mui/icons-material/Bookmark';
import BookmarkBorderIcon from '@mui/icons-material/BookmarkBorder';

export default function ArticleCard({ article, onToggleScrap }) {
  return (
    <Card variant="outlined">
      <CardContent>
        <Stack direction="row" spacing={1} sx={{ mb: 1 }}>
          <Chip
            label={article.source}
            size="small"
            color={article.source === 'GEEK' ? 'primary' : 'secondary'}
          />
          {article.keyword && (
            <Chip label={article.keyword} size="small" variant="outlined" />
          )}
        </Stack>
        <Typography variant="h6" component="h3">
          <MuiLink
            href={article.link}
            target="_blank"
            rel="noopener noreferrer"
            underline="hover"
            color="text.primary"
          >
            {article.title}
          </MuiLink>
        </Typography>
        <Typography
          variant="body2"
          color="text.secondary"
          sx={{
            mt: 1,
            display: '-webkit-box',
            WebkitLineClamp: 2,
            WebkitBoxOrient: 'vertical',
            overflow: 'hidden',
          }}
        >
          {article.description}
        </Typography>
      </CardContent>
      <CardActions sx={{ justifyContent: 'space-between', px: 2, pb: 2 }}>
        <Typography variant="caption" color="text.secondary">
          {article.publishedAt
            ? new Date(article.publishedAt).toLocaleString('ko-KR')
            : ''}
        </Typography>
        <IconButton
          onClick={() => onToggleScrap(article)}
          aria-label={article.isScraped ? '스크랩 해제' : '스크랩'}
        >
          {article.isScraped ? (
            <BookmarkIcon color="primary" />
          ) : (
            <BookmarkBorderIcon />
          )}
        </IconButton>
      </CardActions>
    </Card>
  );
}
