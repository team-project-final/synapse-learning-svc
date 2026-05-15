"""create note_chunks table

Revision ID: 96f1bb4b65ed
Revises: 
Create Date: 2026-05-15 15:30:00.000000

"""
from typing import Sequence, Union

from alembic import op
import sqlalchemy as sa
from pgvector.sqlalchemy import Vector
from sqlalchemy.dialects import postgresql

# revision identifiers, used by Alembic.
revision: str = '96f1bb4b65ed'
down_revision: Union[str, None] = None
branch_labels: Union[str, Sequence[str], None] = None
depends_on: Union[str, Sequence[str], None] = None


def upgrade() -> None:
    # Enable pgvector extension
    op.execute("CREATE EXTENSION IF NOT EXISTS vector")

    op.create_table(
        'note_chunks',
        sa.Column('id', sa.UUID(), nullable=False),
        sa.Column('tenant_id', sa.UUID(), nullable=False),
        sa.Column('note_id', sa.UUID(), nullable=False),
        sa.Column('chunk_index', sa.Integer(), nullable=False),
        sa.Column('content', sa.Text(), nullable=False),
        sa.Column('embedding', Vector(1536), nullable=False),
        sa.Column('embedding_model', sa.String(), nullable=False),
        sa.Column('embedding_version', sa.Integer(), nullable=False, server_default='1'),
        sa.Column('chunk_strategy', sa.String(), nullable=False),
        sa.Column('tokens', sa.Integer(), nullable=True),
        sa.Column('metadata', postgresql.JSON(astext_type=sa.Text()), nullable=False, server_default='{}'),
        sa.Column('created_at', sa.DateTime(), nullable=False, server_default=sa.func.now()),
        sa.PrimaryKeyConstraint('id')
    )
    
    # Indices
    op.create_index('idx_note_chunks_tenant_id', 'note_chunks', ['tenant_id'], unique=False)
    op.create_index('idx_note_chunks_note_id', 'note_chunks', ['note_id'], unique=False)
    
    # HNSW Index for vector search (ERD 5.4)
    # Note: Using op.execute for HNSW with cosine ops as it's cleaner in Alembic
    op.execute(
        "CREATE INDEX idx_note_chunks_embedding ON note_chunks "
        "USING hnsw (embedding vector_cosine_ops) "
        "WITH (m = 16, ef_construction = 64) "
        "WHERE tenant_id IS NOT NULL"
    )


def downgrade() -> None:
    op.drop_index('idx_note_chunks_embedding', table_name='note_chunks')
    op.drop_index('idx_note_chunks_note_id', table_name='note_chunks')
    op.drop_index('idx_note_chunks_tenant_id', table_name='note_chunks')
    op.drop_table('note_chunks')
